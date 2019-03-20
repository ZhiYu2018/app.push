package push.front.api.xmpp;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

public class FcmPacketExtension implements ExtensionElement{
	public static final String FCM_ELEMENT_NAME = "gcm";
	public static final String FCM_NAMESPACE = "google:mobile:data";
	private String json;
	
	public FcmPacketExtension(String json) {
		this.json = json;
	}	
	
	public String getJson(){
		return this.json;
	}
	
	@Override
	public String getElementName() {
		return FCM_ELEMENT_NAME;
	}
	@Override
	public CharSequence toXML(String enclosingNamespace) {
		return String.format("<%s xmlns=\"%s\">%s</%s>", getElementName(), 
				             getNamespace(), json, getElementName());
	}
	@Override
	public String getNamespace() {
		return FCM_NAMESPACE;
	}
	
	public Stanza toPacket() {
		final Message message = new Message();
		message.addExtension(this);
		return message;
	}
}
