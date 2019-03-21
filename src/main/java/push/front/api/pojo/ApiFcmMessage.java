package push.front.api.pojo;

import java.util.Map;
public class ApiFcmMessage {
	private  Map<String, Object> data;
	private  ApiFcmNotification notification;
	private  ApiAndroidConfig androidConfig;
	private  ApiWebpushConfig webpushConfig;
	private  ApiApnsConfig apnsConfig;
	private  String token;
	private  String topic;
	private  String condition;
	private  Boolean dryRun;
	
	public ApiFcmMessage(){
		dryRun = Boolean.FALSE;
	}
	
	public Map<String, Object> getData() {
		return data;
	}
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	public ApiFcmNotification getNotification() {
		return notification;
	}
	public void setNotification(ApiFcmNotification notification) {
		this.notification = notification;
	}
	public ApiAndroidConfig getAndroidConfig() {
		return androidConfig;
	}
	public void setAndroidConfig(ApiAndroidConfig androidConfig) {
		this.androidConfig = androidConfig;
	}
	public ApiWebpushConfig getWebpushConfig() {
		return webpushConfig;
	}
	public void setWebpushConfig(ApiWebpushConfig webpushConfig) {
		this.webpushConfig = webpushConfig;
	}
	public ApiApnsConfig getApnsConfig() {
		return apnsConfig;
	}
	public void setApnsConfig(ApiApnsConfig apnsConfig) {
		this.apnsConfig = apnsConfig;
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
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	public Boolean getDryRun() {
		return dryRun;
	}
	public void setDryRun(Boolean dryRun) {
		this.dryRun = dryRun;
	}
}
