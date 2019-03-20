package push.front.api.xmpp;

public class XmppControl {
	private String message_type;
	private String from;
	private String message_id;
	private String category;
	private String data;
	private String control_type;
	public String getMessage_type() {
		return message_type;
	}
	public void setMessage_type(String message_type) {
		this.message_type = message_type;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getMessage_id() {
		return message_id;
	}
	public void setMessage_id(String message_id) {
		this.message_id = message_id;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getControl_type() {
		return control_type;
	}
	public void setControl_type(String control_type) {
		this.control_type = control_type;
	}
}
