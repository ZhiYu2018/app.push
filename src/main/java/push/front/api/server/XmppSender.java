package push.front.api.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jivesoftware.smack.packet.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import push.front.api.pojo.ApiRequest;
import push.front.api.pojo.ApiResponse;
import push.front.api.xmpp.FcmPacketExtension;
import push.front.api.xmpp.XmppFcm;
import push.front.api.xmpp.XmppMessage;

import com.alibaba.fastjson.JSON;

public class XmppSender implements Sender{
	private static Logger logger = LoggerFactory.getLogger(XmppSender.class);
	private AtomicInteger xmppIdx;
	private List<XmppFcm> xmppFcmList;
	public XmppSender(String userId, String apiKey, boolean prd){
		this.xmppIdx = new AtomicInteger(0);
		this.xmppFcmList = new ArrayList<>();
		XmppFcm.setGlobal();
		int availProcessors = Runtime.getRuntime().availableProcessors();
		for(int cpu = 0; cpu < availProcessors; cpu++){
			XmppFcm xfcm = new XmppFcm(userId, apiKey, prd);
			if(xfcm.connect() == 0){
				xmppFcmList.add(xfcm);
			}
		}
		
		logger.info("Xmpp FCM size:{}:{}", xmppFcmList.size(), availProcessors);
	}

	@Override
	public boolean checkOk() {
		return (!xmppFcmList.isEmpty());
	}

	@Override
	public ApiResponse router(ApiRequest req) {
		ApiResponse apiRsp = new ApiResponse();
		apiRsp.setStat(200);
		apiRsp.setReason("Submit");		
		if(checkOk() == false){
			apiRsp.setStat(500);
			apiRsp.setReason("Xmpp disconnected");
			return apiRsp;
		}
		
		XmppMessage msg = JSON.parseObject(req.getData().toString(), XmppMessage.class);
		String messageId = req.getMsgId();
		for(String token:req.getToken_list()){
			String message_id = XmppFcm.getMessageId(messageId);
			msg.setTo(token);
			msg.setMessage_id(message_id);
			Stanza request = new FcmPacketExtension(JSON.toJSONString(msg)).toPacket();
			XmppFcm fcm = getXmppFcm();			
			fcm.sendStanza(token, message_id, request);
		}
		
		return apiRsp;
	}
	
	private XmppFcm getXmppFcm(){
		int pos = xmppIdx.incrementAndGet();
		if(pos < 0){
			xmppIdx.set(0);
			pos = 0;
		}
		
		for(int p = 0; p < xmppFcmList.size(); p++){
			int idx = pos % xmppFcmList.size();
			XmppFcm fcm = xmppFcmList.get(idx);
			if(fcm.isAlive()){
				return fcm;
			}
		}
		
		/**找默认**/
		int idx = pos % xmppFcmList.size();
		XmppFcm fcm = xmppFcmList.get(idx);
		return fcm;
	}
}
