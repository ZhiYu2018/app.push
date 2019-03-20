package push.front.api.xmpp;

public class XmppResponse {
	private String from;
	private String message_id;
	private String message_type;
	private String registration_id;
	private String error;
	private String error_description;
	
	
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
	public String getMessage_type() {
		return message_type;
	}
	public void setMessage_type(String message_type) {
		this.message_type = message_type;
	}
	public String getRegistration_id() {
		return registration_id;
	}
	public void setRegistration_id(String registration_id) {
		this.registration_id = registration_id;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public String getError_description() {
		return error_description;
	}
	public void setError_description(String error_description) {
		this.error_description = error_description;
	}
	
	
}
