package push.front.api.util;

import com.google.api.client.util.ExponentialBackOff;

public class BackOffStrategy {
	private static final int deFinterval = 500;
	private static final int defMaxInterval = 6000;
	private static final int defMaxElapsedTime = 60000;
	private static final int defMaxRetry = 8;
	private static double deFmultiplier = 1.5;
	private ExponentialBackOff boff;
	private long backOffMill;
	private boolean stopped;
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
		stopped = false;
	}
	
	public boolean shouldRetry(){
		if(boff.getElapsedTimeMillis() < boff.getMaxElapsedTimeMillis()){
			return (stopped == false);
		}
		return false;
	}
	
	
	
	public long getBackOffMillis(){
		return backOffMill;
	}
	
	public void doNotRetry(){
		stopped = true;
	}
	
	public void errorOccured(){
		try{
			backOffMill = boff.nextBackOffMillis();
			if(backOffMill != ExponentialBackOff.STOP){
				Thread.sleep(backOffMill);
			}
		}catch(Throwable t){
			
		}
	}


}
