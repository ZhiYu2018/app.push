package push.front.api.xmpp;

import org.jivesoftware.smack.ReconnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiReconnectionListener implements ReconnectionListener{
	private static Logger logger = LoggerFactory.getLogger(ApiReconnectionListener.class);
	@Override
	public void reconnectingIn(int seconds) {
		logger.info("Reconnection in {} seconds", seconds);
	}

	@Override
	public void reconnectionFailed(Exception e) {
		logger.warn("Reconnection failed! Error: {}", e.getMessage());
	}

}
