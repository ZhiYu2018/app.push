package push.front.api.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
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
	private static final int MAX_QUESIZE = 20480;
	private static Logger logger = LoggerFactory.getLogger(XmppSender.class);
	private AtomicInteger xmppIdx;
	private AtomicInteger sendSeq;
	private List<XmppFcm> xmppFcmList;
	private LinkedBlockingDeque<ApiRequest> deq;
	private ApiThreadFactory factory;
	public XmppSender(String userId, String apiKey, boolean prd){
		this.xmppIdx = new AtomicInteger(0);
		this.sendSeq = new AtomicInteger(0);
		this.xmppFcmList = new ArrayList<>();
		this.deq = new LinkedBlockingDeque<>(MAX_QUESIZE);
		
		XmppFcm.setGlobal();
		int availProcessors = Runtime.getRuntime().availableProcessors();
		logger.info("Xmpp FCM size:{}:{}", xmppFcmList.size(), availProcessors);
		for(int cpu = 0; cpu < availProcessors; cpu++){
			XmppFcm xfcm = new XmppFcm(userId, apiKey, prd);
			if(xfcm.connect() == 0){
				xmppFcmList.add(xfcm);
			}
		}
		
		factory = new ApiThreadFactory(xmppFcmList.size(), "xmpp.sender");
		Runnable r = new Runnable(){
			@Override
			public void run() {
				work();
			}};
		for(int i = 0; i < xmppFcmList.size(); i++){
			Thread th = factory.newThread(r);
			th.start();
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
		
		try{
			deq.put(req);
			return apiRsp;
		}catch(Throwable t){
			apiRsp.setStat(500);
			apiRsp.setReason("It is to busy");
			return apiRsp;
		}
	}
	
	private void work(){
		logger.info("Working .......");
		int sended = 0;
		while(true){
			if(ApiContext.waitQuit(1)){
				break;
			}
			try{
				ApiRequest req = deq.poll(5, TimeUnit.SECONDS);
				if(req == null){
					continue;
				}
				
				XmppMessage msg = JSON.parseObject(req.getData().toString(), XmppMessage.class);
				String messageId = req.getMsgId();
				for(String token:req.getToken_list()){
					String message_id = XmppFcm.getMessageId(messageId);
					msg.setTo(token);
					msg.setMessage_id(message_id);
					Stanza request = new FcmPacketExtension(JSON.toJSONString(msg)).toPacket();
					XmppFcm fcm = getXmppFcm();			
					fcm.sendMsg(token, message_id, request);
					sendSeq.incrementAndGet();
					sended ++;
				}
			}catch(Throwable t){
				logger.info("Poll exceptions:", t);
			}
			
			if(sended >= 1000){
				logger.info("Send msg:{} of {}", sended, sendSeq.get());
				sended = 0;
			}
			
		}
		
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

	@Override
	public void restart() {
		logger.info("xmpp fcm restart");
		for(XmppFcm fcm:xmppFcmList){
			fcm.disconnectAll();
		}
	}
}
