package push.front.api.server;

import java.util.concurrent.ThreadFactory;

public class ApiThreadFactory implements ThreadFactory{
	private int cur;
	private int num;
	private String prefixName;
	private Thread[] pool;
	
	public ApiThreadFactory(int num, String name){
		this.cur = 0;
		this.num = num;
		this.prefixName = name;
		pool = new Thread[num];
	}

	@Override
	public Thread newThread(Runnable r) {
		synchronized(this){
			if(cur >= num){
				return null;
			}
			Thread th = new Thread(r);
			th.setName(String.format("%s.%02d", prefixName, cur));
			pool[cur] = th;
			cur++;
			return th;
		}
	}

}
