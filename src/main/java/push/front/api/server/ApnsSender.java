package push.front.api.server;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.metrics.dropwizard.DropwizardApnsClientMetricsListener;
import com.turo.pushy.apns.util.concurrent.PushNotificationFuture;

import push.front.api.pojo.ApiRequest;
import push.front.api.pojo.ApiResponse;
import push.front.api.pojo.ApnsMessage;
import push.front.api.pojo.Constent;
import push.front.api.pojo.StatInfo;
import push.front.api.util.Util;

public class ApnsSender implements Sender{
	private static Logger logger = LoggerFactory.getLogger(ApnsSender.class);
	private boolean isOk;
	private String prefix;
	private String topic;
	private AtomicLong msgSeq;
	private Semaphore semaphore;
	private ApnsClient apnsClient;
	private Queue<ApnsJob> futureQue;
	private Thread futureThread;
	
	public ApnsSender(String env, String file, String topic, String pwd){
		isOk = false;
		prefix = Util.randomString(5);
		msgSeq = new AtomicLong();
		this.topic = topic;
		semaphore = new Semaphore(1_000);
		futureQue = new PriorityQueue<ApnsJob>(new Comparator<ApnsJob>(){
			@Override
			public int compare(ApnsJob o1, ApnsJob o2) {
				if(o1.getTime() < o2.getTime()){
					return -1;
				}else if(o1.getTime() == o2.getTime()){
					return 0;
				}else{
					return 1;
				}
			}});
		String host = ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
		if(env.equals("Prod")){
			host = ApnsClientBuilder.PRODUCTION_APNS_HOST;
		}
		
		int cpu = Runtime.getRuntime().availableProcessors();
		logger.info("File:{} pwd:{} host:{} cpu:{}", file, pwd.substring(pwd.length() - 4), host, cpu);
		try{
			final DropwizardApnsClientMetricsListener listener = new DropwizardApnsClientMetricsListener();
			EventLoopGroup eventLoopGroup = new NioEventLoopGroup(cpu * 2);
			ApnsClientBuilder acb = new ApnsClientBuilder();
			acb.setApnsServer(host);
			acb.setClientCredentials(new File(file), pwd);
			acb.setConcurrentConnections(cpu*2);
			acb.setEventLoopGroup(eventLoopGroup);
			acb.setMetricsListener(listener);
			acb.setConnectionTimeout(Constent.CONNECT_TIME_OUT, TimeUnit.MILLISECONDS);
			apnsClient = acb.build();
			isOk = true;
			futureThread = new Thread(new Runnable(){
				@Override
				public void run() {
					poolCheck();
				}});
			futureThread.setName("future.apns");
			futureThread.start();
			logger.info("start apns ok");
		}catch(Throwable t){
			logger.error("start apns", t);
			isOk = false;
		}
	}
	@Override
	public boolean checkOk() {
		return isOk;
	}

	@Override
	public ApiResponse router(ApiRequest req) {
		ApnsMessage msg = JSON.parseObject(req.getData().toString(), ApnsMessage.class);
		msg.setTopic(topic);
		PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> future = null;
		for(String token:req.getToken_list()){
			try{
				semaphore.acquire();
			}catch(Throwable t){}
			msg.setToken(token);
			ApnsPushNotification notification = Util.buildApnsMessage(msg);
			ApnsJob job = new ApnsJob();
			String msgId = String.format("%s-%d.%s", prefix, msgSeq.incrementAndGet(), msg.getMessageId());
			job.setCustomId(msg.getMessageId());
			job.setMsgId(msgId);
			job.setToken(token);
			job.setTopic(msg.getTopic());
			future = apnsClient.sendNotification(notification);
			future.addListener(new AnpsFutureListener(job));
			job.setFuture(future);
			pushJob(job);
		}
		return null;
	}
	
	private void poolCheck(){
		List<ApnsJob> jobList = new ArrayList<>();
		while(true){
			if(ApiContext.waitQuit(Constent.MAX_TIME_INTERVAL)){
				break;
			}
			
			/**get jobs**/
			peekJob(jobList);
			
			/**check jobs**/
			checkJob(jobList);
			
			if(!jobList.isEmpty()){
				jobList.clear();
			}
		}
	}
	
	private void peekJob(List<ApnsJob> jobList){
		int max = 100;
		long now = System.currentTimeMillis()/1000;
		synchronized(futureQue){
			while(jobList.size() < max){
				ApnsJob job = futureQue.poll();
				if(job == null){
					break;
				}
				if(job.getTime() > now){
					futureQue.add(job);
					break;
				}
				
				jobList.add(job);
			}
		}
	}
	
	private void checkJob(List<ApnsJob> jobList){
		PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> future;
		for(ApnsJob job:jobList){
			future = job.getFuture();
			if(!future.isDone()){
				if(job.checkTimeOut()){
					logger.info("Message {}:{}:{}:{} is time out:{}", job.getCustomId(), 
							    job.getMsgId(), job.getToken(), job.getTopic(), job.getRetryCount());
					statMsg(job, Constent.Time_Out, "Response time out");
					/**释放**/
					semaphore.release();
				}else{
					pushJob(job);
				}
			}else{
				semaphore.release();
				try{
					if(future.isSuccess()){
						statMsg(job, Constent.OK_RSP, job.getDesp());
					}else{
						logger.info("Message {}:{}:{}:{} is fail:{}", job.getCustomId(), 
							         job.getMsgId(), job.getToken(), job.getTopic(), job.getDesp());
						statMsg(job, Constent.FAILED_RSP, job.getDesp());
					}
				}catch(Throwable t){
					logger.error("Message {}:{}:{} get exceptions:", job.getMsgId(), job.getToken(), job.getTopic(), t);
				}
			}
		}
	}
	
	private void pushJob(ApnsJob job){
		synchronized(futureQue){
			futureQue.add(job);
		}
	}
	
	private void statMsg(ApnsJob job, String errorCode, String desp){
		ApiStat.get().push(new StatInfo(job.getCustomId(), job.getMsgId(), 
		           Util.getDateTimeStr(System.currentTimeMillis()),
		           job.getToken(), errorCode, desp));
	}
	

}
