package push.front.api.pojo;

public enum MethodType {
	APNS("apns"),
	FCM_HTTP("fcm.http"),
	FCM_XMPP("fcm.xmpp"),
	FCM_ADMIN("fcm.sdk");
	private final String type;
	private MethodType(String type){
		this.type = type;
	}
	public String getType() {
		return type;
	}

}
