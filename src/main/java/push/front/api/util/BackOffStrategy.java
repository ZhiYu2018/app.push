package push.front.api.util;

import com.google.api.client.util.ExponentialBackOff;

public class BackOffStrategy {
	private static final int deFinterval = 500;
	private static final int defMaxInterval = 1000*60;
	private static final int defMaxElapsedTime = 1000*60*5;
	private static final int defMaxRetry = 8;
	private static double deFmultiplier = 1.5;
	private ExponentialBackOff boff;
	private long backOffMill;
	private final int maxTry;
	private int count;
	public BackOffStrategy() {
		this(defMaxRetry, deFinterval, deFmultiplier);
	}
	
	public BackOffStrategy(int maxTry, long interval, double maxAttempts){
		ExponentialBackOff.Builder bld = new ExponentialBackOff.Builder();
		bld.setInitialIntervalMillis(deFinterval);
		bld.setMaxElapsedTimeMillis(defMaxElapsedTime);
		bld.setMultiplier(deFmultiplier);
		bld.setMaxIntervalMillis(defMaxInterval);
		boff = bld.build();
		this.maxTry = maxTry;
		count = 0;
	}
	
	public boolean shouldRetry(){
		return (count < maxTry);
	}
	
	
	
	public long getBackOffMillis(){
		return backOffMill;
	}
	
	public void doNotRetry(){
		count = maxTry;
	}
	
	public void errorOccured(){
		try{
			count ++;
			backOffMill = boff.nextBackOffMillis();
			if(backOffMill != ExponentialBackOff.STOP){
				Thread.sleep(backOffMill);
			}
		}catch(Throwable t){
			
		}
	}


}
