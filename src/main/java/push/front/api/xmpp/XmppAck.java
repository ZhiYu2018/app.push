package push.front.api.xmpp;

public class XmppAck {
	private String to;
	private String message_id;
	private String message_type;
	public XmppAck(String to, String message_id, String message_type) {
		super();
		this.to = to;
		this.message_id = message_id;
		this.message_type = message_type;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getMessage_id() {
		return message_id;
	}
	public void setMessage_id(String message_id) {
		this.message_id = message_id;
	}
	public String getMessage_type() {
		return message_type;
	}
	public void setMessage_type(String message_type) {
		this.message_type = message_type;
	}
}
