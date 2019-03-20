package push.front.api.pojo;

import java.util.Map;

import com.turo.pushy.apns.DeliveryPriority;

public class ApnsMessage {
	private String customId;
	private String token;
	private String invalidationTime;
	private int priority;
	private String topic;
	private String collapseId;
	private String messageId;
	private Map<String, Object> payload;
	public ApnsMessage() {
		super();
		priority = DeliveryPriority.IMMEDIATE.getCode();
		
	}
		
	public String getCustomId() {
		return customId;
	}
	public void setCustomId(String customId) {
		this.customId = customId;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public Map<String, Object> getPayload() {
		return payload;
	}
	public void setPayload(Map<String, Object> payload) {
		this.payload = payload;
	}
	public String getInvalidationTime() {
		return invalidationTime;
	}
	public void setInvalidationTime(String invalidationTime) {
		this.invalidationTime = invalidationTime;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getCollapseId() {
		return collapseId;
	}
	public void setCollapseId(String collapseId) {
		this.collapseId = collapseId;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
}
