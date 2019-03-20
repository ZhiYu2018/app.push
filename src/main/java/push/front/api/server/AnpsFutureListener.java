package push.front.api.server;


import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.PushNotificationResponse;

public class AnpsFutureListener implements GenericFutureListener<Future<PushNotificationResponse<ApnsPushNotification>>>{
	private final ApnsJob job;
	public AnpsFutureListener(ApnsJob job){
		this.job = job;
	}
	
	@Override
	public void operationComplete(
			Future<PushNotificationResponse<ApnsPushNotification>> future)
			throws Exception {
		if(future.isDone()){
			String status = ((future.get().isAccepted())? "Accepted":"Reject");
			job.setState(status);
			job.setDesp(future.get().getRejectionReason());
		}else{
			job.setState("Unknow");
			job.setDesp("Unknow");
		}
	}

}
