package push.front.api.pojo;

public class ApiResponse {
	private String serialId;
	private int stat;
	private String reason;
	private Object data;
	public String getSerialId() {
		return serialId;
	}
	public void setSerialId(String serialId) {
		this.serialId = serialId;
	}
	public int getStat() {
		return stat;
	}
	public void setStat(int stat) {
		this.stat = stat;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
}
