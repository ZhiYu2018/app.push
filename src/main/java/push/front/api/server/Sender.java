package push.front.api.server;

import push.front.api.pojo.ApiRequest;
import push.front.api.pojo.ApiResponse;

public interface Sender {
	public boolean checkOk();
	public ApiResponse router(ApiRequest req);
	public void restart();
}
