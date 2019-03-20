package push.front.api.server;

import push.front.api.pojo.Constent;
import push.front.api.util.Util;

import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.concurrent.PushNotificationFuture;

public class ApnsJob {
	private long time;
	private int retryCount;
	private String customId;
	private String msgId;
	private String token;
	private String topic;
	private String state;
	private String desp;
	private PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> future;
	public ApnsJob(){
		retryCount = 1;
		this.time = System.currentTimeMillis()/1000 + Util.getWaitTimeExp(retryCount, 1);
	}
	
	public boolean checkTimeOut(){
		retryCount = retryCount + 1;
		if(retryCount > Constent.MAX_TIMES){
			return true;
		}
		
		this.time = System.currentTimeMillis()/1000 + Util.getWaitTimeExp(retryCount, 1);
		return false;
	}
	
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public int getRetryCount() {
		return retryCount;
	}
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	public String getCustomId() {
		return customId;
	}
	public void setCustomId(String customId) {
		this.customId = customId;
	}
	
	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getDesp() {
		return desp;
	}
	public void setDesp(String desp) {
		this.desp = desp;
	}
	public PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> getFuture() {
		return future;
	}
	public void setFuture(
			PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> future) {
		this.future = future;
	}
}
