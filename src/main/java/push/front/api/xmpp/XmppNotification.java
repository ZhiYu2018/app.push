package push.front.api.xmpp;

public class XmppNotification {
	/**fcm 抽象的ios和安卓共有的字段**/
	private String title;
	private String body;
	private String sound;
	private String click_action;
	private String body_loc_key;
	private String body_loc_args;
	private String title_loc_key;
	private String title_loc_args;
	
	/**ios:专有**/
	private String badge;
	private String subtitle;
	
	/**android:专有**/
	private String android_channel_id;
	private String icon;
	private String tag;
	private String color;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getSound() {
		return sound;
	}
	public void setSound(String sound) {
		this.sound = sound;
	}
	public String getClick_action() {
		return click_action;
	}
	public void setClick_action(String click_action) {
		this.click_action = click_action;
	}
	public String getBody_loc_key() {
		return body_loc_key;
	}
	public void setBody_loc_key(String body_loc_key) {
		this.body_loc_key = body_loc_key;
	}
	public String getBody_loc_args() {
		return body_loc_args;
	}
	public void setBody_loc_args(String body_loc_args) {
		this.body_loc_args = body_loc_args;
	}
	public String getTitle_loc_key() {
		return title_loc_key;
	}
	public void setTitle_loc_key(String title_loc_key) {
		this.title_loc_key = title_loc_key;
	}
	public String getTitle_loc_args() {
		return title_loc_args;
	}
	public void setTitle_loc_args(String title_loc_args) {
		this.title_loc_args = title_loc_args;
	}
	public String getBadge() {
		return badge;
	}
	public void setBadge(String badge) {
		this.badge = badge;
	}
	public String getSubtitle() {
		return subtitle;
	}
	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}
	public String getAndroid_channel_id() {
		return android_channel_id;
	}
	public void setAndroid_channel_id(String android_channel_id) {
		this.android_channel_id = android_channel_id;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
}
