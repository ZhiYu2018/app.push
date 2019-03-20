package push.front.api.pojo;

import java.util.Map;

public class ApiApnsConfig {
	private Map<String, String> headers;
	private Map<String, Object> payload;
	public Map<String, String> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	public Map<String, Object> getPayload() {
		return payload;
	}
	public void setPayload(Map<String, Object> payload) {
		this.payload = payload;
	}
	
}
