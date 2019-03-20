package push.front.api.xmpp;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import push.front.api.server.XmppSender;

public class ApiConnectionListener implements ConnectionListener{
	private final XmppSender xmppSender;
	public ApiConnectionListener(XmppSender xmppSender){
		this.xmppSender = xmppSender;
	}
	private static Logger logger = LoggerFactory.getLogger(ApiConnectionListener.class);
	@Override
	public void connected(XMPPConnection connection) {
		logger.info("Connection established.");
	}

	@Override
	public void authenticated(XMPPConnection connection, boolean resumed) {
		logger.info("authenticated resumed:{}", resumed);
		xmppSender.onUserAuthentication();
	}

	@Override
	public void connectionClosed() {
		logger.info("connectionClosed");
		xmppSender.reconnect();
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		logger.info("connectionClosedOnError:", e);
	}

}
