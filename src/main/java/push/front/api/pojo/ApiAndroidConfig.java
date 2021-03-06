package push.front.api.pojo;

import java.util.Map;

public class ApiAndroidConfig {
	private String collapseKey;
	private String priority;
	private long ttl;
	private String restrictedPackageName;
	private Map<String, String> data;
	private ApiAndroidNotification notification;
	
	public ApiAndroidConfig(){
		ttl = 2419200L;
	}
	
	public String getCollapseKey() {
		return collapseKey;
	}
	public void setCollapseKey(String collapseKey) {
		this.collapseKey = collapseKey;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public long getTtl() {
		return ttl;
	}
	public void setTtl(long ttl) {
		this.ttl = ttl;
	}
	public String getRestrictedPackageName() {
		return restrictedPackageName;
	}
	public void setRestrictedPackageName(String restrictedPackageName) {
		this.restrictedPackageName = restrictedPackageName;
	}
	public Map<String, String> getData() {
		return data;
	}
	public void setData(Map<String, String> data) {
		this.data = data;
	}
	public ApiAndroidNotification getNotification() {
		return notification;
	}
	public void setNotification(ApiAndroidNotification notification) {
		this.notification = notification;
	}
}
