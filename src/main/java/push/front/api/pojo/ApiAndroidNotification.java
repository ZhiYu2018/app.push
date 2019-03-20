package push.front.api.pojo;

import java.util.List;
public class ApiAndroidNotification {
	private String title;
	private String body;
	private String icon;
	private String color;
	private String sound;
	private String tag;
	private String clickAction;
	private String bodyLocKey;
	private List<String> bodyLocArgs;
	private String titleLocKey;
	private List<String> titleLocArgs;
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
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public String getSound() {
		return sound;
	}
	public void setSound(String sound) {
		this.sound = sound;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getClickAction() {
		return clickAction;
	}
	public void setClickAction(String clickAction) {
		this.clickAction = clickAction;
	}
	public String getBodyLocKey() {
		return bodyLocKey;
	}
	public void setBodyLocKey(String bodyLocKey) {
		this.bodyLocKey = bodyLocKey;
	}
	public List<String> getBodyLocArgs() {
		return bodyLocArgs;
	}
	public void setBodyLocArgs(List<String> bodyLocArgs) {
		this.bodyLocArgs = bodyLocArgs;
	}
	public String getTitleLocKey() {
		return titleLocKey;
	}
	public void setTitleLocKey(String titleLocKey) {
		this.titleLocKey = titleLocKey;
	}
	public List<String> getTitleLocArgs() {
		return titleLocArgs;
	}
	public void setTitleLocArgs(List<String> titleLocArgs) {
		this.titleLocArgs = titleLocArgs;
	}
	
	
}
