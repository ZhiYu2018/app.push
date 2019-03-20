package push.front.api.xmpp;

import java.util.Map;

/****
 * @author simon
 * Rule:https://firebase.google.com/docs/cloud-messaging/xmpp-server-ref#send-downstream
 *
 */

public class XmppMessage {
	/**可选**/
	private String to;
	/**可选**/
	private String condition;
	private String message_id;
	private String collapse_key;
	private String priority;
	/**IOS**/
	private boolean content_available;
	private boolean mutable_content;
	private long time_to_live;
	private boolean delivery_receipt_requested;
	private boolean dry_run;
	private XmppNotification notification;
	private Map<String,Object> data;
	public XmppMessage(){
		this.priority = "normal";
		this.time_to_live = 4 * 7 * 3600L;
		this.dry_run = false;
	}
	
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	public String getMessage_id() {
		return message_id;
	}
	public void setMessage_id(String message_id) {
		this.message_id = message_id;
	}
	public String getCollapse_key() {
		return collapse_key;
	}
	public void setCollapse_key(String collapse_key) {
		this.collapse_key = collapse_key;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public boolean isContent_available() {
		return content_available;
	}
	public void setContent_available(boolean content_available) {
		this.content_available = content_available;
	}
	public boolean isMutable_content() {
		return mutable_content;
	}
	public void setMutable_content(boolean mutable_content) {
		this.mutable_content = mutable_content;
	}
	public long getTime_to_live() {
		return time_to_live;
	}
	public void setTime_to_live(long time_to_live) {
		this.time_to_live = time_to_live;
	}
	public boolean isDelivery_receipt_requested() {
		return delivery_receipt_requested;
	}
	public void setDelivery_receipt_requested(boolean delivery_receipt_requested) {
		this.delivery_receipt_requested = delivery_receipt_requested;
	}
	public boolean isDry_run() {
		return dry_run;
	}
	public void setDry_run(boolean dry_run) {
		this.dry_run = dry_run;
	}
	public XmppNotification getNotification() {
		return notification;
	}
	public void setNotification(XmppNotification notification) {
		this.notification = notification;
	}
	public Map<String, Object> getData() {
		return data;
	}
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	
}
