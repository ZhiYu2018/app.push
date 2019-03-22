package push.front.api.server;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ApiContext {
	private static volatile boolean bQuit = false;
	private static final Semaphore available = new Semaphore(0, true);
	public static boolean waitQuit(int sec){
		if(!bQuit){
			try{
				available.tryAcquire(sec, TimeUnit.SECONDS);
			}catch(Throwable t){
				
			}
			try{Thread.sleep(200);}catch(Throwable t){}
		}
		return bQuit;
	}
	
	public static void setQuit(){
		bQuit = true;
		/**通知退出**/
		available.release(128);
	}
}
