package push.front.api.server;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import push.front.api.pojo.ApiRequest;
import push.front.api.pojo.ApiResponse;
import push.front.api.pojo.Constent;
import push.front.api.pojo.MethodType;

@Service
public class SenderRouter {
	private static Logger logger = LoggerFactory.getLogger(SenderRouter.class);
	private Map<String, Sender> senderMap;
	
	@Autowired
	public SenderRouter(Environment env){
		senderMap = new HashMap<>();
		
		String conf = env.getProperty(Constent.API_PUSH_FILE);
		logger.info("{} is {}", Constent.API_PUSH_FILE, conf);
		Properties prop = new Properties();
		try(FileInputStream fis = new FileInputStream(conf)){
			prop.load(fis);
		}catch(Throwable t){
			logger.error("load {}, exceptions:", conf, t);
			return ;
		}
		
		/**init firebase admin**/
		String accFile = prop.getProperty(Constent.FCM_ACC_FILE);
		Sender sender = new FSdkSender(accFile);
		if(sender.checkOk()){
			logger.info("Add {} sender", MethodType.FCM_ADMIN.getType());
			senderMap.put(MethodType.FCM_ADMIN.getType(), sender);
		}
		
		/**apns**/
		String keyFile = prop.getProperty(Constent.APNS_KEY_FILE);
		String pwd = prop.getProperty(Constent.APNS_KEY_PWD);
		String apnsEnv = prop.getProperty(Constent.APNS_ENV);
		String topic = prop.getProperty(Constent.APNS_TOPIC);
		sender = new ApnsSender(apnsEnv, keyFile, topic, pwd);
		if(sender.checkOk()){
			logger.info("Add {} sender", MethodType.APNS.getType());
			senderMap.put(MethodType.APNS.getType(), sender);
		}
		
		/**xmpp**/
		String xmppUser = prop.getProperty(Constent.XMPP_USER);
		String xmppKey  = prop.getProperty(Constent.XMPP_KEY);
		String xmppEnv  = prop.getProperty(Constent.XMPP_ENV);
		sender = new XmppSender(xmppUser, xmppKey, (xmppEnv.equalsIgnoreCase("prod")));
		if(sender.checkOk()){
			logger.info("Add {} sender", MethodType.FCM_XMPP.getType());
			senderMap.put(MethodType.FCM_XMPP.getType(), sender);
		}
		
	}
	
	public ApiResponse router(ApiRequest req){
		ApiResponse rsp = new ApiResponse();
		Sender sender = senderMap.get(req.getMethod());
		if(sender == null){
			rsp.setStat(500);
			rsp.setSerialId(req.getMsgId());
			rsp.setReason("Inernation error");
			return rsp;
		}
		
		rsp = sender.router(req);
		logger.info("Msg:{} state:{}, reason:{}", req.getMsgId(), 
				    rsp.getStat(), 
				    rsp.getReason());
		return rsp;
	}
	
	

}
