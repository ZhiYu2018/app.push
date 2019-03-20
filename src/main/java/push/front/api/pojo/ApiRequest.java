package push.front.api.pojo;

import java.util.List;

public class ApiRequest{
	private String msgId;
	private String method; //anps/http-fcm/xmpp-fcm/admin-fcm/ali
	private String data;
	private List<String> token_list;
	public String getMsgId() {
		return msgId;
	}
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public List<String> getToken_list() {
		return token_list;
	}
	public void setToken_list(List<String> token_list) {
		this.token_list = token_list;
	}
}
