package push.front.api.xmpp;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLContext;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
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
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.RateLimiter;

import push.front.api.pojo.Constent;
import push.front.api.pojo.StatInfo;
import push.front.api.server.ApiStat;
import push.front.api.util.BackOffStrategy;
import push.front.api.util.Util;

public class XmppFcm implements ConnectionListener, ReconnectionListener,PingFailedListener,StanzaListener{
	private static final String FCM_SERVER = "fcm-xmpp.googleapis.com";
	private static final int FCM_PROD_PORT = 5235;
	private static final int FCM_TEST_PORT = 5236;
	private static final int MAX_RATE_SECOND = 500;
	private static final int PENDING_SIZE = 100;
	private static final String FCM_SERVER_AUTH_CONNECTION = "gcm.googleapis.com";
	private static Logger logger = LoggerFactory.getLogger(XmppFcm.class);
	private static final String prefix = Util.randomString(5);
	private static final AtomicLong msgSeq = new AtomicLong();
	private static final AtomicLong msgSender = new AtomicLong();
	private static final RateLimiter rateLimiter = RateLimiter.create(MAX_RATE_SECOND);
	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
	
	private String userId;
	private String apiKey;
	private boolean prd;
	private volatile boolean isConnectionDraining;
	//private volatile int pingFailedTimes;
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
	
	private final Map<String, Jobs> pendingMessages;
	private final Map<String, Jobs> reTryMessages;
	
	public static void setGlobal(){
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
		Base64.setEncoder(new ApiB64Encoder());
	}
	
	public static String getMessageId(String customId){
		String message_id = String.format("%s-%d.%s", prefix, msgSeq.incrementAndGet(), customId);
		return message_id;
	}
	
	
	public XmppFcm(String userId, String apiKey, boolean prd){
		this.userId = userId + "@" + FCM_SERVER_AUTH_CONNECTION;;
		this.apiKey = apiKey;
		this.prd    = prd;
		this.xmppConnection = null;
		this.isConnectionDraining = true;
		//this.pingFailedTimes = 0;
		this.pendingMessages = new ConcurrentHashMap<>();
		this.reTryMessages = new ConcurrentHashMap<>(); 
		
	}
	
	public boolean isAlive() {
		XMPPTCPConnection con = xmppConnection;
		if(con != null){
			return (con.isConnected() && con.isAuthenticated());
		}
		return false;
	}
	
	public int connect(){
		XMPPTCPConnection xmppConn = null;
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
		    int port = 0;
			if(this.prd){
				port = FCM_PROD_PORT;
				
			}else{
				port = FCM_TEST_PORT;
			}
			config.setPort(port);
			logger.info("Connecting to xmpp:{}:{},{},port:{}", this.userId, this.apiKey, 
					    serviceName.toString(), port);
			xmppConn = new XMPPTCPConnection(config.build());
			ReconnectionManager.getInstanceFor(xmppConn).enableAutomaticReconnection();
		    ReconnectionManager.getInstanceFor(xmppConn).addReconnectionListener(this);

		    Roster.getInstanceFor(xmppConn).setRosterLoadedAtLogin(false);
		    
		    // Security checks
		    SASLAuthentication.blacklistSASLMechanism("X-OAUTH2"); // FCM CCS requires a SASL PLAIN authentication mechanism
		    SASLAuthentication.blacklistSASLMechanism("X-GOOGLE-TOKEN");
		    SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());
		    logger.info("SASL PLAIN authentication enabled ? {}", SASLAuthentication.isSaslMechanismRegistered("PLAIN"));
		    logger.info("Is compression enabled ? {}", xmppConn.isUsingCompression());
		    logger.info("Is the connection secure ? {}", xmppConn.isSecureConnection());
		    
		    // add listener
		    xmppConn.addConnectionListener(this);
		    xmppConn.addAsyncStanzaListener(this, new StanzaFilter(){
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
		    pingManager.setPingInterval(60);
		    pingManager.registerPingFailedListener(this);
		    /**add listen**/
		    xmppConn.connect();
		    xmppConn.login();
			logger.info("Connected to xmpp:{}", this.userId);
			xmppConnection = xmppConn;
			return 0;
		}catch(Throwable t){
			logger.error("Connect to xmpp, exceptions:", t);
			return -1;
		}
	}
	
	public void sendMsg(String token, String message_id, Stanza request){
		Jobs job = new Jobs();
		job.msgId = message_id;
		job.token = token;
		job.stanza = request;
		sendStanza(job);
	}
	
	private void sendStanza(Jobs job){
		BackOffStrategy backoff = new BackOffStrategy();
		int r = -1;
		while(backoff.shouldRetry()){
			try{
				if((this.isConnectionDraining == false) 
					&& (this.pendingMessages.size() < PENDING_SIZE)){
					/**流控**/
					rateLimiter.acquire();
					xmppConnection.sendStanza(job.stanza);
					backoff.doNotRetry();
					r = 0;
				}
			}catch(Throwable t){
				logger.error("Send msgId:{},exceptions:", job.msgId, t);
			}
			//error occured, and in back off
			if(backoff.shouldRetry()){
				backoff.errorOccured();
			}
		}
		
		if(r != 0){
			logger.warn("Put message to retry q, for:{},{}", isConnectionDraining, pendingMessages.size());
			reTryMessages.put(job.msgId, job);
		}else{
			pendingMessages.put(job.msgId, job);
		}
	}

	
	private void onUserAuthentication(){
		logger.info("Pending:{}, Sync:{}", reTryMessages.size(), pendingMessages.size());
		Map<String, Jobs> syncJobs = new HashMap<>(pendingMessages);
		Map<String, Jobs> pendingJobs = new HashMap<>(reTryMessages);
		
		/**clear**/
		pendingMessages.clear();
		reTryMessages.clear();
		
		syncJobs.putAll(pendingJobs);
		for(Map.Entry<String, Jobs> kv :syncJobs.entrySet()){
			Jobs job = kv.getValue();
			if(job.timestamp < (System.currentTimeMillis() - 1000 * 60L)){
				StatInfo si = new StatInfo(job.msgId, kv.getKey(),
                        Util.getDateTimeStr(System.currentTimeMillis()),
                        job.token, Constent.FAILED_RSP, "Drop old");
				ApiStat.get().push(si);
				continue;
			}else{
				sendStanza(job);
			}
		}
			
	}
	
	private void setConnectionDraining(){
		logger.info("FCM Connection is draining!");
		isConnectionDraining = true;
	}
	
	private void removePendingJob(String msgId){
		pendingMessages.remove(msgId);
	}
	
	private void handleUpstreamMessage(XmppInMessage inMsg){
    	logger.info("Received: Type {}, From:{}",inMsg.getCategory(), inMsg.getFrom());
        XmppAck ack = new XmppAck(inMsg.getFrom(), inMsg.getMessage_id(), "ack");
        Stanza packet = new FcmPacketExtension(JSON.toJSONString(ack)).toPacket();
        BackOffStrategy backoff = new BackOffStrategy();
        while(backoff.shouldRetry()){
        	try{
        		if(this.isConnectionDraining == false){
        			if(this.isAlive()){
        				xmppConnection.sendStanza(packet);
        				backoff.doNotRetry();
        			}
        		}
        	}catch(Throwable t){
        		;
        	}
        	if(backoff.shouldRetry()){
        		backoff.errorOccured();
        	}
        }
	}
	
	private void disconnectAll() {
		XMPPTCPConnection xmppConn = this.xmppConnection;
	    if (xmppConn.isConnected()) {
	        logger.info("Detaching all the listeners for the connection.");
	        PingManager.getInstanceFor(xmppConn).unregisterPingFailedListener(this);
	        ReconnectionManager.getInstanceFor(xmppConn).removeReconnectionListener(this);
	        xmppConn.removeAsyncStanzaListener(this);
	        xmppConn.removeConnectionListener(this);
	        xmppConn.removeStanzaInterceptor(this);
	        xmppConn.removeAllRequestAckPredicates();
	        xmppConn.removeAllStanzaAcknowledgedListeners();
	        xmppConn.removeAllStanzaIdAcknowledgedListeners();
	        xmppConn.removeStanzaSendingListener(this);
	        xmppConn.removeStanzaAcknowledgedListener(this);
	        xmppConn.removeAllRequestAckPredicates();
	        logger.info("Disconnecting the xmpp server from FCM.");
	        xmppConn.disconnect();
	      }
	}
	
	private synchronized void reconnect(){
		logger.info("Reconnect ......");
		BackOffStrategy backoff = new BackOffStrategy();
		int r = -1;
		while(backoff.shouldRetry()){
			if(this.connect() == 0){
				backoff.doNotRetry();
				r = 0;
			}else{
				backoff.errorOccured();
			}
		}
		
		if(r != 0){
			logger.warn("Reconnect failed.");
			/**重连**/
			executor.schedule(new Runnable(){
				@Override
				public void run() {
					reconnect();
			}}, 500, TimeUnit.MILLISECONDS);
		}else{
			logger.info("Reconnect success.");
		}
	}
	 
	@Override
	public void connected(XMPPConnection connection) {
		logger.info("Connection established.");
	}

	@Override
	public void authenticated(XMPPConnection connection, boolean resumed) {
		logger.info("authenticated resumed:{}", resumed);
		isConnectionDraining = false;
		onUserAuthentication();
	}

	@Override
	public void connectionClosed() {
		logger.info("connectionClosed");
		/**重连**/
		executor.schedule(new Runnable(){
			@Override
			public void run() {
				reconnect();
			}}, 500, TimeUnit.MILLISECONDS);
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		logger.info("connectionClosedOnError error:", e);
	}

	@Override
	public void pingFailed() {
		logger.info("The ping failed, restarting the ping interval again ...");
		final PingManager pingManager = PingManager.getInstanceFor(xmppConnection);
		pingManager.setPingInterval(60);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void processStanza(Stanza packet) throws NotConnectedException,
			InterruptedException, NotLoggedInException {
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
                String customId = Util.getCustomId(resp.getMessage_id());
                if(messageType.equals("ack")){
                	si = new StatInfo(customId, 
                		              resp.getMessage_id(),
                		              Util.getDateTimeStr(System.currentTimeMillis()),
                		              resp.getFrom(), Constent.OK_RSP, "ack");
                }else{
                	si = new StatInfo(customId, resp.getMessage_id(),
      		                          Util.getDateTimeStr(System.currentTimeMillis()),
      		                          resp.getFrom(), Constent.FAILED_RSP, resp.getError());
                	if((resp.getError() != null) && (resp.getError().equals("CONNECTION_DRAINING"))){
                		setConnectionDraining();
                	}
                }
                ApiStat.get().push(si);
                removePendingJob(resp.getMessage_id());
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
            	String customId = Util.getCustomId(ctrl.getMessage_id());
            	StatInfo si = new StatInfo(customId, 
            			                   ctrl.getMessage_id(),
  		                                   Util.getDateTimeStr(System.currentTimeMillis()),
  		                                   token, Constent.OK_RSP, "receipt");
            	ApiStat.get().push(si);
            	removePendingJob(ctrl.getMessage_id());
            	return ;
			}
			
			if(messageType.equals("control")){
				XmppControl ctrl = JSON.parseObject(fcmPacket.getJson(), XmppControl.class);
            	logger.info("Received: Type:{}, From:{}, category:{}, data:{}, control_type:{}",
        			         ctrl.getMessage_type(), ctrl.getFrom(), 
        			         ctrl.getCategory(), ctrl.getData(),
        			         ctrl.getControl_type());
            	if((ctrl.getControl_type() != null) && ctrl.getControl_type().equals("CONNECTION_DRAINING")){
            		setConnectionDraining();
            	}
			}
		}
	}

	@Override
	public void reconnectingIn(int seconds) {
		logger.info("Reconnecting in {} ...", seconds);
	}

	@Override
	public void reconnectionFailed(Exception e) {
		logger.info("Reconnection failed! Error: {}", e.getMessage());
	}

}
