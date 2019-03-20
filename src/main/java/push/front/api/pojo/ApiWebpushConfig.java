package push.front.api.pojo;

import java.util.Map;

public class ApiWebpushConfig {
	private Map<String, String> headers;
	private Map<String, String> data;
	private ApiWebpushNotification notification;
	public Map<String, String> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	public Map<String, String> getData() {
		return data;
	}
	public void setData(Map<String, String> data) {
		this.data = data;
	}
	public ApiWebpushNotification getNotification() {
		return notification;
	}
	public void setNotification(ApiWebpushNotification notification) {
		this.notification = notification;
	}
}
