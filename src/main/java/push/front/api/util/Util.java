package push.front.api.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.alibaba.fastjson.JSON;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.DeliveryPriority;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;

import push.front.api.pojo.ApiAndroidNotification;
import push.front.api.pojo.ApiFcmMessage;
import push.front.api.pojo.ApiWebpushConfig;
import push.front.api.pojo.ApnsMessage;
import push.front.api.pojo.MethodType;

public class Util {
	private final static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyz" +
                                               "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();
	private final static Map<String, MethodType> METHOD_SUPPOERTED = new HashMap<>();
	private static Random randGen = new Random();
	static {
		METHOD_SUPPOERTED.put(MethodType.APNS.getType(), MethodType.APNS);
		METHOD_SUPPOERTED.put(MethodType.FCM_HTTP.getType(), MethodType.FCM_HTTP);
		METHOD_SUPPOERTED.put(MethodType.FCM_XMPP.getType(), MethodType.FCM_XMPP);
		METHOD_SUPPOERTED.put(MethodType.FCM_ADMIN.getType(), MethodType.FCM_ADMIN);
	}
	
	public static MethodType getMethod(String m){	
		return METHOD_SUPPOERTED.get(m);
	}
	
    public static String randomString(int length) {
        if (length < 1) {
            return null;
        }
        // Create a char buffer to put random letters and numbers in.
        char [] randBuffer = new char[length];
        for (int i=0; i<randBuffer.length; i++) {
            randBuffer[i] = numbersAndLetters[randGen.nextInt(71)];
        }
        return new String(randBuffer);
    }
    
    public static long getWaitTimeExp(int retryCount, long baseTimes){
    	long TempWait = 0;
    	TempWait = (long)(Math.pow(2, retryCount) * baseTimes);
    	TempWait = (TempWait - 1)/2;
    	if(TempWait <= 1){
    		TempWait = 1;
    	}
    	
    	return TempWait;
    }
    
    public static boolean strEqual(String src, String dst){
    	if((src == null) || (dst == null)){
    		return false;
    	}
    	
    	return src.equals(dst);
    }
    
    public static Message buileFcmMessage(ApiFcmMessage msg){
		Message.Builder builder = Message.builder();
		
		// notification
		if(msg.getNotification() != null){
			Notification notification = new Notification(msg.getNotification().getTitle(), 
					                                     msg.getNotification().getBody());
			builder.setNotification(notification);
		}
		
		//android config
		if(msg.getAndroidConfig() != null){
			AndroidConfig.Builder acBuilder = AndroidConfig.builder();
			acBuilder.setCollapseKey(msg.getAndroidConfig().getCollapseKey());
			if(msg.getAndroidConfig().getPriority() != null){
				if(strEqual(msg.getAndroidConfig().getPriority(), "HIGH")){
					acBuilder.setPriority(AndroidConfig.Priority.HIGH);
				}else{
					acBuilder.setPriority(AndroidConfig.Priority.NORMAL);
				}
			}
			if(msg.getAndroidConfig().getNotification() != null){
				ApiAndroidNotification apiAnf = msg.getAndroidConfig().getNotification();
				AndroidNotification anf = AndroidNotification.builder().setBody(apiAnf.getBody())
						                  .setBodyLocalizationKey(apiAnf.getBodyLocKey())
						                  .setClickAction(apiAnf.getClickAction())
						                  .setColor(apiAnf.getColor())
						                  .setIcon(apiAnf.getIcon())
						                  .setSound(apiAnf.getSound())
						                  .setTag(apiAnf.getTag())
						                  .setTitle(apiAnf.getTitle())
						                  .setTitleLocalizationKey(apiAnf.getTitleLocKey())
						                  .addAllBodyLocalizationArgs(apiAnf.getBodyLocArgs())
						                  .addAllTitleLocalizationArgs(apiAnf.getTitleLocArgs())
						                  .build();
				acBuilder.setNotification(anf);			                  
			}
			
			acBuilder.setRestrictedPackageName(msg.getAndroidConfig().getRestrictedPackageName());
			acBuilder.setTtl(msg.getAndroidConfig().getTtl());
			builder.setAndroidConfig(acBuilder.build());
		}
		
		//webpushconfig
		if(msg.getWebpushConfig() != null){
			ApiWebpushConfig wbpush = msg.getWebpushConfig();
			WebpushConfig.Builder bld = WebpushConfig.builder();
			if(wbpush.getNotification() != null){
				bld.setNotification(new WebpushNotification(wbpush.getNotification().getTitle(), 
						wbpush.getNotification().getBody(), wbpush.getNotification().getIcon()));
			}
			if(wbpush.getData() != null){
				bld.putAllData(wbpush.getData());
			}
			if(wbpush.getHeaders() != null){
				bld.putAllHeaders(wbpush.getHeaders());
			}
			
			builder.setWebpushConfig(bld.build());
		}
		
		//apnsconig
		if(msg.getApnsConfig() != null){
			ApnsConfig.Builder bld = ApnsConfig.builder();
			if(msg.getApnsConfig().getHeaders() != null){
				bld.putAllHeaders(msg.getApnsConfig().getHeaders());
			}
			if(msg.getApnsConfig().getPayload() != null){
				bld.putAllCustomData(msg.getApnsConfig().getPayload());
			}
			
			builder.setApnsConfig(bld.build());
		}
		
		// token
		builder.setToken(msg.getToken());
		builder.setTopic(msg.getTopic());
		if(!msg.getData().isEmpty()){
			for(Map.Entry<String, Object> obj: msg.getData().entrySet()){
				builder.putData(obj.getKey(), obj.getValue().toString());
			}
		}
		builder.setCondition(msg.getCondition());
		return builder.build();
    }
    
    public static ApnsPushNotification buildApnsMessage(ApnsMessage msg){
    	Date invalidationTime = new Date(System.currentTimeMillis() + SimpleApnsPushNotification.DEFAULT_EXPIRATION_PERIOD_MILLIS);
    	if(msg.getInvalidationTime() != null){
    		 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    		 try{
    			 invalidationTime = sdf.parse(msg.getInvalidationTime());
    		 }catch(Throwable t){
    			 
    		 }
    	}
    	DeliveryPriority priority = DeliveryPriority.IMMEDIATE;
    	if(msg.getPriority() == DeliveryPriority.CONSERVE_POWER.getCode()){
    		priority = DeliveryPriority.CONSERVE_POWER;
    	}
    	
    	String payLoad = JSON.toJSONString(msg.getPayload());
    	ApnsPushNotification notify = new SimpleApnsPushNotification(msg.getToken(),
    			                                                     msg.getTopic(),
    			                                                     payLoad,
    			                                                     invalidationTime,
    			                                                     priority,
    			                                                     msg.getCollapseId());
    	return notify;
    }
    
    public static MulticastMessage builderMultMessage(List<String> tokens, ApiFcmMessage msg){
    	MulticastMessage.Builder builder = MulticastMessage.builder();
    	// token 
    	builder.addAllTokens(tokens);
    	
    	// data
		if(!msg.getData().isEmpty()){
			for(Map.Entry<String, Object> obj: msg.getData().entrySet()){
				builder.putData(obj.getKey(), obj.getValue().toString());
			}
		}
    	
		// notification
		if(msg.getNotification() != null){
			Notification notification = new Notification(msg.getNotification().getTitle(), 
					                                     msg.getNotification().getBody());
			builder.setNotification(notification);
		}
		
		//android config
		if(msg.getAndroidConfig() != null){
			AndroidConfig.Builder acBuilder = AndroidConfig.builder();
			acBuilder.setCollapseKey(msg.getAndroidConfig().getCollapseKey());
			if(msg.getAndroidConfig().getPriority() != null){
				if(strEqual(msg.getAndroidConfig().getPriority(), "HIGH")){
					acBuilder.setPriority(AndroidConfig.Priority.HIGH);
				}else{
					acBuilder.setPriority(AndroidConfig.Priority.NORMAL);
				}
			}
			if(msg.getAndroidConfig().getNotification() != null){
				ApiAndroidNotification apiAnf = msg.getAndroidConfig().getNotification();
				AndroidNotification anf = AndroidNotification.builder().setBody(apiAnf.getBody())
						                  .setBodyLocalizationKey(apiAnf.getBodyLocKey())
						                  .setClickAction(apiAnf.getClickAction())
						                  .setColor(apiAnf.getColor())
						                  .setIcon(apiAnf.getIcon())
						                  .setSound(apiAnf.getSound())
						                  .setTag(apiAnf.getTag())
						                  .setTitle(apiAnf.getTitle())
						                  .setTitleLocalizationKey(apiAnf.getTitleLocKey())
						                  .addAllBodyLocalizationArgs(apiAnf.getBodyLocArgs())
						                  .addAllTitleLocalizationArgs(apiAnf.getTitleLocArgs())
						                  .build();
				acBuilder.setNotification(anf);			                  
			}
			
			acBuilder.setRestrictedPackageName(msg.getAndroidConfig().getRestrictedPackageName());
			acBuilder.setTtl(msg.getAndroidConfig().getTtl());
			builder.setAndroidConfig(acBuilder.build());
		}
		
		// web push
		if(msg.getWebpushConfig() != null){
			ApiWebpushConfig wbpush = msg.getWebpushConfig();
			WebpushConfig.Builder bld = WebpushConfig.builder();
			if(wbpush.getNotification() != null){
				bld.setNotification(new WebpushNotification(wbpush.getNotification().getTitle(), 
						wbpush.getNotification().getBody(), wbpush.getNotification().getIcon()));
			}
			if(wbpush.getData() != null){
				bld.putAllData(wbpush.getData());
			}
			if(wbpush.getHeaders() != null){
				bld.putAllHeaders(wbpush.getHeaders());
			}
			
			builder.setWebpushConfig(bld.build());
		}
		
		// apns
		if(msg.getApnsConfig() != null){
			ApnsConfig.Builder bld = ApnsConfig.builder();
			if(msg.getApnsConfig().getHeaders() != null){
				bld.putAllHeaders(msg.getApnsConfig().getHeaders());
			}
			if(msg.getApnsConfig().getPayload() != null){
				bld.putAllCustomData(msg.getApnsConfig().getPayload());
			}
			
			builder.setApnsConfig(bld.build());
		}   	
    	
    	return builder.build();
    }
    
    public static String getCustomId(String msgId){
    	int idx = msgId.indexOf(".");
    	if(idx < 0){
    		return msgId;
    	}
    	
    	return msgId.substring(idx + 1);
    }
    
	public static String getDateTimeStr(long ms){
		synchronized(Util.class){
			Date aDate = new Date(ms);
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTime(aDate); 
			String str = String.format("%04d-%02d-%02d %02d:%02d:%02d.%03d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
				                   calendar.get(Calendar.DATE), calendar.get(Calendar.HOUR_OF_DAY),
				                   calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
				                   calendar.get(Calendar.MILLISECOND));
			return str;	
		}
	}

}
