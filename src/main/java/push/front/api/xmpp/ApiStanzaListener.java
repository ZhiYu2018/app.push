package push.front.api.xmpp;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;

import push.front.api.server.XmppSender;

public class ApiStanzaListener implements StanzaListener{
	private final XmppSender sender;
	
	public ApiStanzaListener(XmppSender sender){
		this.sender = sender;
	}

	@Override
	public void processStanza(Stanza packet) throws NotConnectedException,
			InterruptedException, NotLoggedInException {
		sender.processStanza(packet);
	}

}
