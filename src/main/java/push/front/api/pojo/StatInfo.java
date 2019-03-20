package push.front.api.pojo;

public class StatInfo {
	private String time;
	private String customId;
	private String msgId;
	private String token;
	private String status;
	private String reason;
	
	public StatInfo(String customId, String msgId, String time, 
			        String token, String status, String reason) {
		super();
		this.customId = customId;
		this.msgId = msgId;
		this.time = time;
		this.token = token;
		this.status = status;
		this.reason = reason;
	}
	
	public String getMsgId() {
		return msgId;
	}
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public byte[] toByte(){
		StringBuilder sb = new StringBuilder();
		sb.append(time).append("|");
		sb.append(customId).append("|");
		sb.append(msgId).append("|");
		sb.append(token).append("|");
		sb.append(status).append("|");
		sb.append(reason).append("\n");
		return sb.toString().getBytes();
	}
}
