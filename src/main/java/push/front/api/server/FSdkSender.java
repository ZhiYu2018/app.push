package push.front.api.server;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
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
import push.front.api.util.Util;

public class FSdkSender implements Sender{
	//private static final String SUCCESS_RETURN_PREFIX = "projects/";
	private static Logger logger = LoggerFactory.getLogger(FSdkSender.class);
	private boolean isOk;
	//private String prefix;
	//private AtomicLong msgSeq;
	private RateLimiter rateLimiter;
	private Thread futureThread;
	private class Job{
		private long time;
		private int retryCount;
		private ApiFuture<BatchResponse> future;
		
		public Job(ApiFuture<BatchResponse> future) {
			super();
			this.time = System.currentTimeMillis()/1000 + Util.getWaitTimeExp(retryCount, 1);
			this.retryCount = 0;
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
		//prefix = Util.randomString(5);
		//msgSeq = new AtomicLong();
		rateLimiter = RateLimiter.create(500);
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
		List<Message> messages = new ArrayList<>();
		for(String token:req.getToken_list()){
			msg.setToken(token);
			messages.add(Util.buileFcmMessage(msg));
			rateLimiter.acquire();
			if(messages.size() >= 200){
				ApiFuture<BatchResponse> future = FirebaseMessaging.getInstance().sendAllAsync(messages, msg.getDryRun());
				Job job = new Job(future);
				pushJob(job);
				messages.clear();
			}
		}
		
		if(!messages.isEmpty()){
			ApiFuture<BatchResponse> future = FirebaseMessaging.getInstance().sendAllAsync(messages, msg.getDryRun());
			Job job = new Job(future);
			pushJob(job);
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
							    response.getSuccessCount(), response.getFailureCount());
					List<SendResponse> lst = response.getResponses();
					for(SendResponse rs:lst){
						if(rs.isSuccessful()){
							logger.info("Success:{}", rs.getMessageId());
						}else{
							logger.error("Failed:{}", rs.getException().getErrorCode());
						}
					}
					
				}catch(Throwable t){
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
}
