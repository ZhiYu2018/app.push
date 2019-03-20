package push.front.api.server;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLContext;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sasl.javax.SASLPlainMechanism;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.RateLimiter;

import push.front.api.pojo.ApiRequest;
import push.front.api.pojo.ApiResponse;
import push.front.api.pojo.Constent;
import push.front.api.pojo.StatInfo;
import push.front.api.util.BackOffStrategy;
import push.front.api.util.Util;
import push.front.api.xmpp.ApiB64Encoder;
import push.front.api.xmpp.ApiConnectionListener;
import push.front.api.xmpp.ApiPingFailedListener;
import push.front.api.xmpp.ApiReconnectionListener;
import push.front.api.xmpp.ApiStanzaListener;
import push.front.api.xmpp.FcmPacketExtension;
import push.front.api.xmpp.XmppAck;
import push.front.api.xmpp.XmppControl;
import push.front.api.xmpp.XmppInMessage;
import push.front.api.xmpp.XmppMessage;
import push.front.api.xmpp.XmppResponse;

public class XmppSender implements Sender{
	private static final String FCM_SERVER = "fcm-xmpp.googleapis.com";
	private static final int FCM_PROD_PORT = 5235;
	private static final int FCM_TEST_PORT = 5236;
	private static final String FCM_SERVER_AUTH_CONNECTION = "gcm.googleapis.com";
	private static final int MAX_RATE_SECOND = 500;
	private static Logger logger = LoggerFactory.getLogger(XmppSender.class);
	private volatile boolean reconnecting;
	private volatile boolean isConnectionDraining;
	private final String prefix;
	private final AtomicLong msgSeq;
	private final AtomicLong msgSender;
	private final String userId;
	private final String apiKey;
	private final boolean prd;
	private boolean isOk;
	private RateLimiter rateLimiter;
	private volatile XMPPTCPConnection xmppConnection;
	
	private class Jobs{
		private long timestamp;
		private String msgId;
		private String token;
		private Stanza stanza;
		public Jobs(){
			timestamp = System.currentTimeMillis();
		}
	}
	
	private final Map<String, Jobs> syncMessages;
	private final Map<String, Jobs> pendingMessages;
	
	public XmppSender(String userId, String apiKey, boolean prd){
		this.reconnecting = false;
		this.isConnectionDraining = true;
		this.prefix = Util.randomString(5);
		this.msgSeq = new AtomicLong();
		this.msgSender = new AtomicLong();
		this.userId = userId + "@" + FCM_SERVER_AUTH_CONNECTION;;
		this.apiKey = apiKey;
		this.prd    = prd;
		rateLimiter = RateLimiter.create(MAX_RATE_SECOND);
		syncMessages = new ConcurrentHashMap<>();
		pendingMessages = new ConcurrentHashMap<>();
		ProviderManager.addExtensionProvider(FcmPacketExtension.FCM_ELEMENT_NAME, 
				                             FcmPacketExtension.FCM_NAMESPACE,
		                                     new ExtensionElementProvider<FcmPacketExtension>() {
		          @Override
		          public FcmPacketExtension parse(XmlPullParser parser, int initialDepth)
		              throws XmlPullParserException, IOException, SmackException {
		            final String json = parser.nextText();
		            return new FcmPacketExtension(json);
		          }
		        });
		
		/*****/
		Base64.setEncoder(new ApiB64Encoder());
		isOk = false;
		connect();
	}

	@Override
	public boolean checkOk() {
		return isOk;
	}

	@Override
	public ApiResponse router(ApiRequest req) {
		ApiResponse apiRsp = new ApiResponse();
		apiRsp.setStat(200);
		apiRsp.setReason("Submit");
		
		synchronized(this){
			if(isConnectionDraining){
				isOk = false;
				disconnect();
				doReconnect();
			}
		}
		
		if(isOk == false){
			apiRsp.setStat(500);
			apiRsp.setReason("Xmpp disconnected");
			return apiRsp;
		}
		
		XmppMessage msg = JSON.parseObject(req.getData().toString(), XmppMessage.class);
		String messageId = req.getMsgId();
		for(String token:req.getToken_list()){
			String message_id = String.format("%s-%d.%s", prefix, msgSeq.incrementAndGet(), messageId);
			msg.setTo(token);
			msg.setMessage_id(message_id);
			Stanza request = new FcmPacketExtension(JSON.toJSONString(msg)).toPacket();
			Jobs stanza = new Jobs();
			stanza.msgId = req.getMsgId();
			stanza.token = token;
			stanza.stanza = request;
			int r = doSendStanza(message_id, stanza);
			if(r != 0){
				StatInfo si = new StatInfo(req.getMsgId(), 
						                    message_id,
						                    Util.getDateTimeStr(System.currentTimeMillis()),
						                    token,
						                    Constent.FAILED_RSP,
						                    "Net exceptions");
				ApiStat.get().push(si);
				/**put to pending message**/
				pendingMessages.put(message_id, stanza);
			}else{
				/**put to sync message**/
				syncMessages.put(message_id, stanza);
			}
		}
		
		return apiRsp;
	}
	
	public void disconnect(){
		if((xmppConnection != null) && (xmppConnection.isConnected())){
			xmppConnection.disconnect();
		}
	}
	
	private int doSendStanza(String msgId, Jobs request){
		rateLimiter.acquire();
		
		/**异常时候采用指数规避算法**/
		BackOffStrategy backoff = new BackOffStrategy();
		int r = -1;
		while (backoff.shouldRetry()){
			try{
				xmppConnection.sendStanza(request.stanza);
				backoff.doNotRetry();
				r = 0;
			}catch(Throwable t){
				logger.error("Send msgId:{},exceptions:", msgId, t);
				backoff.errorOccured();
			}
		}
		return r;
	}
	
	private int connect(){
		XMPPTCPConnection xmppConn = null;
		xmppConnection = null;
		try{
			DomainBareJid serviceName = JidCreate.domainBareFrom("gcm.googleapis.com");
			XMPPTCPConnection.setUseStreamManagementResumptionDefault(true);
			XMPPTCPConnection.setUseStreamManagementDefault(true);
			final SSLContext sslContext = SSLContext.getInstance("TLS");
		    sslContext.init(null, null, new SecureRandom());
		    SmackConfiguration.DEBUG = false;
		    InetAddress addr = InetAddress.getByName(FCM_SERVER);
		    
			XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
			config.setXmppDomain(serviceName);
		    config.setHostAddress(addr);
		    config.setSendPresence(false);
		    config.setSecurityMode(SecurityMode.ifpossible);
		    config.setCompressionEnabled(true);
		    config.setSocketFactory(sslContext.getSocketFactory());
		    config.setCustomSSLContext(sslContext);
		    config.setUsernameAndPassword(userId, apiKey);
			if(this.prd){
				config.setPort(FCM_PROD_PORT);
			}else{
				config.setPort(FCM_TEST_PORT);
			}
			
			logger.info("Connecting to xmpp:{}:{},{}", this.userId, this.apiKey, serviceName.toString());
			xmppConn = new XMPPTCPConnection(config.build());
			ReconnectionManager.getInstanceFor(xmppConn).enableAutomaticReconnection();
		    ReconnectionManager.getInstanceFor(xmppConn).addReconnectionListener(new ApiReconnectionListener());

		    Roster.getInstanceFor(xmppConn).setRosterLoadedAtLogin(false);
		    
		    // Security checks
		    SASLAuthentication.blacklistSASLMechanism("X-OAUTH2"); // FCM CCS requires a SASL PLAIN authentication mechanism
		    SASLAuthentication.blacklistSASLMechanism("X-GOOGLE-TOKEN");
		    SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());
		    logger.info("SASL PLAIN authentication enabled ? {}", SASLAuthentication.isSaslMechanismRegistered("PLAIN"));
		    logger.info("Is compression enabled ? {}", xmppConn.isUsingCompression());
		    logger.info("Is the connection secure ? {}", xmppConn.isSecureConnection());
		    
		    // add listener
		    xmppConn.addConnectionListener(new ApiConnectionListener(this));
		    xmppConn.addAsyncStanzaListener(new ApiStanzaListener(this), new StanzaFilter(){
				@Override
				public boolean accept(Stanza stanza) {
					return stanza.hasExtension(FcmPacketExtension.FCM_ELEMENT_NAME, 
							                   FcmPacketExtension.FCM_NAMESPACE);
				}});
		    
		    xmppConn.addStanzaInterceptor(new StanzaListener(){
				@Override
				public void processStanza(Stanza packet)
						throws NotConnectedException, InterruptedException,
						NotLoggedInException {
					if(packet.hasExtension(FcmPacketExtension.FCM_ELEMENT_NAME, 
							               FcmPacketExtension.FCM_NAMESPACE)){
						long send = msgSender.incrementAndGet();
						long seq = msgSeq.get();
						logger.debug("seq:{}, send:{}, {}", seq, send, packet.toXML(""));
					}
				}}, ForEveryStanza.INSTANCE);
		    
		    final PingManager pingManager = PingManager.getInstanceFor(xmppConn);
		    pingManager.setPingInterval(100);
		    pingManager.registerPingFailedListener(new ApiPingFailedListener(this, pingManager));
		    
		    /**add listen**/
		    xmppConn.connect();
		    xmppConn.login();
			logger.info("Connected to xmpp:{}", this.userId);
			xmppConnection = xmppConn;
			isConnectionDraining = false;
			isOk = true;
			return 0;
		}catch(Throwable t){
			logger.error("Connect to xmpp, exceptions:", t);
			return -1;
		}
	}
	
	
	public void onUserAuthentication(){
		logger.info("Pending:{}, Sync:{}", pendingMessages.size(), syncMessages.size());
		Map<String, Jobs> syncJobs = new HashMap<>(syncMessages);
		Map<String, Jobs> pendingJobs = new HashMap<>(pendingMessages);
		
		/**clear**/
		syncMessages.clear();
		pendingMessages.clear();
		
		syncJobs.putAll(pendingJobs);
		for(Map.Entry<String, Jobs> kv :syncJobs.entrySet()){
			Jobs job = kv.getValue();
			if(job.timestamp < (System.currentTimeMillis() - 1000 * 60L)){
				StatInfo si = new StatInfo(job.msgId, kv.getKey(),
                        Util.getDateTimeStr(System.currentTimeMillis()),
                        job.token, Constent.FAILED_RSP, "Drop old");
				ApiStat.get().push(si);
				continue;
			}
			
			int r = doSendStanza(kv.getKey(), job);
			if(r != 0){
				StatInfo si = new StatInfo(job.msgId, kv.getKey(),
                                       Util.getDateTimeStr(System.currentTimeMillis()),
                                       job.token, Constent.FAILED_RSP, "Net exceptions");
				ApiStat.get().push(si);
				/***put to pending**/
				pendingMessages.put(kv.getKey(), job);
			}else{
				/***put to sync message**/
				syncMessages.put(kv.getKey(), job);
			}
		}
	}
	
	public void setConnectionDraining(){
		isConnectionDraining = true;
	}
	
	public synchronized void reconnect(){
		if(reconnecting == false){
			reconnecting = true;
			doReconnect();
			reconnecting = false;
		}		
	}
	
	private void doReconnect(){
		BackOffStrategy backoff = new BackOffStrategy();
		while (backoff.shouldRetry()){
			 if(connect() == 0){
				 backoff.doNotRetry();
				 logger.info("reconnect ok");
			 }
			 backoff.errorOccured();
		}
	}
	
	private void removeAsyncJob(String msgId){
		this.syncMessages.remove(msgId);
	}
	
	@SuppressWarnings("unchecked")
	public void processStanza(Stanza packet){
		FcmPacketExtension fcmPacket = (FcmPacketExtension) packet.getExtension(FcmPacketExtension.FCM_NAMESPACE);
		Map<String, Object> map = new HashMap<String, Object>();
		map = JSON.parseObject(fcmPacket.getJson(), map.getClass());
		Optional<Object> messageTypeObj = Optional.ofNullable(map.get("message_type"));
		if(!messageTypeObj.isPresent()){
			handleUpstreamMessage(JSON.parseObject(fcmPacket.getJson(), XmppInMessage.class));
		}else{
			String messageType = messageTypeObj.get().toString();
			if(messageType.equals("ack") || messageType.equals("nack")){
				XmppResponse resp = JSON.parseObject(fcmPacket.getJson(), XmppResponse.class);
                logger.info("Received: Message_id:{}, Type:{}, From:{}, Error:{}" ,
                		    resp.getMessage_id(),resp.getMessage_type(),resp.getFrom(), 
                		    resp.getError());
                StatInfo si = null;
                if(messageType.equals("ack")){
                	si = new StatInfo(resp.getMessage_id(), 
                		              resp.getMessage_id(),
                		              Util.getDateTimeStr(System.currentTimeMillis()),
                		              resp.getFrom(), Constent.OK_RSP, "ack");
                }else{
                	si = new StatInfo(resp.getMessage_id(), 
      		              resp.getMessage_id(),
      		              Util.getDateTimeStr(System.currentTimeMillis()),
      		              resp.getFrom(), Constent.FAILED_RSP, resp.getError());
                	if((resp.getError() != null) && (resp.getError().equals("CONNECTION_DRAINING"))){
                		this.setConnectionDraining();
                	}
                }
                ApiStat.get().push(si);
                removeAsyncJob(resp.getMessage_id());
                return ;
			}
			if(messageType.equals("receipt")){
				XmppControl ctrl = JSON.parseObject(fcmPacket.getJson(), XmppControl.class);
            	logger.info("Received: Type:{}, From:{}, category:{}, data:{}",
        			         ctrl.getMessage_type(), ctrl.getFrom(), 
        			         ctrl.getCategory(), ctrl.getData());
            	Map<String, Object> data = new HashMap<>();
            	data = JSON.parseObject(ctrl.getData(), data.getClass());
            	String token = data.getOrDefault("device_registration_id", ctrl.getFrom()).toString();
            	StatInfo si = new StatInfo(ctrl.getMessage_id(), 
            			                   ctrl.getMessage_id(),
  		                                   Util.getDateTimeStr(System.currentTimeMillis()),
  		                                   token, Constent.OK_RSP, "receipt");
            	ApiStat.get().push(si);
            	removeAsyncJob(ctrl.getMessage_id());
            	return ;
			}
			
			if(messageType.equals("control")){
				XmppControl ctrl = JSON.parseObject(fcmPacket.getJson(), XmppControl.class);
            	logger.info("Received: Type:{}, From:{}, category:{}, data:{}, control_type:{}",
        			         ctrl.getMessage_type(), ctrl.getFrom(), 
        			         ctrl.getCategory(), ctrl.getData(),
        			         ctrl.getControl_type());
            	if((ctrl.getControl_type() != null) && ctrl.getControl_type().equals("CONNECTION_DRAINING")){
            		this.setConnectionDraining();
            	}
			}
		}
	}
	
	private void handleUpstreamMessage(XmppInMessage inMsg){
    	logger.info("Received: Type {}, From:{}",inMsg.getCategory(),
    			inMsg.getFrom());
        XmppAck ack = new XmppAck(inMsg.getFrom(), inMsg.getMessage_id(), "ack");
        Stanza packet = new FcmPacketExtension(JSON.toJSONString(ack)).toPacket();
        try{
        	xmppConnection.sendStanza(packet);
        }catch(Throwable t){
        	logger.error("Send msg {}:{} ack, exceptions", inMsg.getFrom(), inMsg.getMessage_id(), t);
        }
	}

}
