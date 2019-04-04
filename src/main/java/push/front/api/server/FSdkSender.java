package push.front.api.server;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.util.concurrent.RateLimiter;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;

import push.front.api.pojo.ApiFcmMessage;
import push.front.api.pojo.ApiRequest;
import push.front.api.pojo.ApiResponse;
import push.front.api.pojo.Constent;
import push.front.api.pojo.StatInfo;
import push.front.api.util.Util;

public class FSdkSender implements Sender{
	private final static int MAX_RATE = 500;
	private final static int MAX_BATCH = 50;
	private final static String SUCCESS_PREFIX = "/projects/";
	private static Logger logger = LoggerFactory.getLogger(FSdkSender.class);
	private boolean isOk;
	private final String prefix;
	private final AtomicLong msgSeq;
	private RateLimiter rateLimiter;
	private Thread futureThread;
	private class Job{
		private long time;
		private int retryCount;
		private String customId;
		private List<String> tokenList;
		private List<String> msgIdsList;
		private ApiFuture<BatchResponse> future;
		
		public Job(String cid, 
				   List<String> tokens, 
				   List<String> msgIds, 
				   ApiFuture<BatchResponse> future) {
			super();
			this.time = System.currentTimeMillis()/1000 + Util.getWaitTimeExp(retryCount, 1);
			this.retryCount = 0;
			this.customId  = cid;
			this.tokenList = new ArrayList<>(tokens);
			this.msgIdsList = new ArrayList<>(msgIds);
			this.future = future;
		}
		
		public boolean checkTimeOut(){
			retryCount = retryCount + 1;
			if(retryCount > Constent.MAX_TIMES){
				return true;
			}
			
			this.time = System.currentTimeMillis()/1000 + Util.getWaitTimeExp(retryCount, 1);
			return false;
		}
		
	}
	
	private Queue<Job> futureQue;
	
	public FSdkSender(String accFile){
		isOk = false;
		this.prefix = Util.randomString(5);
		this.msgSeq = new AtomicLong();
		rateLimiter = RateLimiter.create(MAX_RATE);
		try(FileInputStream serviceAccount = new FileInputStream(accFile)){
			isOk = true;
			FirebaseOptions.Builder bd = new FirebaseOptions.Builder();
			bd.setCredentials(GoogleCredentials.fromStream(serviceAccount));
			bd.setConnectTimeout(Constent.CONNECT_TIME_OUT);
			bd.setReadTimeout(Constent.READ_TIME_OUT);
			FirebaseOptions options = bd.build();
			FirebaseApp.initializeApp(options);
			
			futureQue = new PriorityQueue<Job>(new Comparator<Job>(){
				@Override
				public int compare(Job o1, Job o2) {
					if(o1.time < o2.time){
						return -1;
					}else if(o1.time == o2.time){
						return 0;
					}else{
						return 1;
					}
				}});
			
			futureThread = new Thread(new Runnable(){
				@Override
				public void run() {
					poolCheck();
				}});
			futureThread.setName("future.fsdk");
			futureThread.start();
			logger.info("start fcm ok");
		}catch(Throwable t){
			logger.error("FSdk start failed:{}", t.getMessage());
			isOk = false;
		}
		
	}

	@Override
	public ApiResponse router(ApiRequest req) {
		ApiResponse apiRsp = new ApiResponse();
		apiRsp.setStat(200);
		apiRsp.setReason("Submit");
		ApiFcmMessage msg = JSON.parseObject(req.getData().toString(), ApiFcmMessage.class);
		List<String> tokens = new ArrayList<>();
		List<String> msgIds = new ArrayList<>();
		List<Message> messages = new ArrayList<>();
		for(String token:req.getToken_list()){
			tokens.add(token);
			String message_id = String.format("%s-%d.%s", prefix, msgSeq.incrementAndGet(), req.getMsgId());
			msgIds.add(message_id);
			msg.setToken(token);
			messages.add(Util.buileFcmMessage(msg));
			
			if(messages.size() >= MAX_BATCH){
				rateLimiter.acquire(messages.size());
				boolean dryRun = msg.getDryRun();
				ApiFuture<BatchResponse> future = FirebaseMessaging.getInstance().sendAllAsync(messages, dryRun);
				Job job = new Job(req.getMsgId(), tokens, msgIds, future);
				pushJob(job);
				
				tokens.clear();
				msgIds.clear();
				/**new a new**/
				messages = new ArrayList<>();
			}
		}
		
		if(!messages.isEmpty()){
			rateLimiter.acquire(messages.size());
			boolean dryRun = msg.getDryRun();
			ApiFuture<BatchResponse> future = FirebaseMessaging.getInstance().sendAllAsync(messages, dryRun);
			Job job = new Job(req.getMsgId(), tokens, msgIds, future);
			pushJob(job);
			
			tokens.clear();
			msgIds.clear();
		}
		
		return apiRsp;
	}

	@Override
	public boolean checkOk() {
		return isOk;
	}
	
	private void pushJob(Job job){
		synchronized(futureQue){
			futureQue.add(job);
		}
	}
	
	private void poolCheck(){
		List<Job> jobList = new ArrayList<>();
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
	
	private void checkJob(List<Job> jobList){
		for(Job job: jobList){
			ApiFuture<BatchResponse> future = job.future;
			if(!future.isDone()){
				if(job.checkTimeOut()){
					logger.info("Message {}:{}:{}:{} is time out:{}");
				}else{
					pushJob(job);
				}
			}else{
				try{
					BatchResponse response = future.get();
					logger.info("Success:{}, Failed:{}", 
							    response.getSuccessCount(), 
							    response.getFailureCount());
					List<SendResponse> lst = response.getResponses();
					int i = 0;
					for(SendResponse rs:lst){
						StatInfo si = null;
						if(rs.isSuccessful()){
							String success = (rs.getMessageId().startsWith(SUCCESS_PREFIX)? "success":"failed");
							si = new StatInfo(job.customId, job.msgIdsList.get(i),
              		                          Util.getDateTimeStr(System.currentTimeMillis()),
              		                          job.tokenList.get(i), 
              		                          Constent.OK_RSP, success);
							logger.info("Send to {}: MessagId:{}, Success:{}", 
									     job.tokenList.get(i),
									     job.tokenList.get(i),
									     rs.getMessageId());
						}else{
							si = new StatInfo(job.customId, job.msgIdsList.get(i),
    		                                  Util.getDateTimeStr(System.currentTimeMillis()),
    		                                  job.tokenList.get(i), 
    		                                  Constent.FAILED_RSP,
    		                                  rs.getException().getErrorCode());
							logger.error("Send to {}: MessagId:{}, Failed:{}", 
								         job.tokenList.get(i),
								         job.tokenList.get(i),
								         rs.getException().getErrorCode());
						}
						
						ApiStat.get().push(si);
						i = i + 1;
					}
					
				}catch(Throwable t){
					logger.info("check exceptions:", t);
				}
			}
		}
	}
	
	private void peekJob(List<Job> jobList){
		int max = 100;
		long now = System.currentTimeMillis()/1000;
		synchronized(futureQue){
			while(jobList.size() < max){
				Job job = futureQue.poll();
				if(job == null){
					break;
				}
				if(job.time > now){
					futureQue.add(job);
					break;
				}
				
				jobList.add(job);
			}
		}
	}

	@Override
	public void restart() {
		logger.info("fsdk reconnect");
	}
}
