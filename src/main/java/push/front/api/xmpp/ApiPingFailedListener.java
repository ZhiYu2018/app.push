package push.front.api.xmpp;

import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import push.front.api.server.XmppSender;

public class ApiPingFailedListener implements PingFailedListener{
	private static Logger logger = LoggerFactory.getLogger(ApiPingFailedListener.class);
	private final PingManager pingManager;
	private final XmppSender sender;

	public ApiPingFailedListener(XmppSender sender,PingManager pingManager){
		this.pingManager = pingManager;
		this.sender = sender;
	}
	@Override
	public void pingFailed() {
		logger.info("The ping failed, restarting the ping interval again ...");
		pingManager.setPingInterval(90);
		sender.setConnectionDraining();
	}

}
