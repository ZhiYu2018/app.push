package push.front.api.server;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import push.front.api.pojo.StatInfo;
import push.front.api.util.Util;

public class ApiStat {
	private static final Logger LOG = LoggerFactory.getLogger(ApiStat.class);
	private static final long MAX_FILE_TIME = 1000*1800L;
	private static volatile ApiStat BCM_STAT = null;
	private volatile String today;
	private volatile int curFileNo;
	private volatile long lastOpen;
	private LinkedBlockingDeque<StatInfo> statQ;
	private Thread thStat;
	private ApiStat(){
		today = "1917-01-01";
		curFileNo = 0;
		lastOpen  = 0;
		statQ = new LinkedBlockingDeque<>();
		thStat = new Thread(new Runnable(){
			@Override
			public void run() {
				dump();
			}});
		thStat.setName("api.push.stat");
		thStat.start();
	}
	
	public static ApiStat get(){
		if(BCM_STAT == null){
			synchronized(ApiStat.class){
				if(BCM_STAT == null){
					BCM_STAT = new ApiStat();
				}
			}
		}
		return BCM_STAT;
	}
	
	public void push(StatInfo job){
		statQ.offer(job);
	}
	
	private void dump(){
		LOG.info("Dump file ......");
		while(true){
			FileOutputStream fos = null;
			try{
				StatInfo job = statQ.poll(30, TimeUnit.SECONDS);
				if(job != null){
					/**写文件**/
					fos = getFileOut();
					if(fos != null){
						byte[] content = job.toByte();
						fos.write(content, 0, content.length);
						while(!statQ.isEmpty()){
							job = statQ.poll();
							content = job.toByte();
							fos.write(content, 0, content.length);
						}
					}
					else{
						/**概率极低**/
						LOG.info("Get output stream error");
					}
				}
			}catch(Throwable t){
				LOG.info("dump excetion", t);
			}
			/**close the stream**/
			if(fos != null){
				try{
					fos.close();
				}catch(Throwable t){
					
				}
			}
		}
	}
	
	private FileOutputStream getFileOut(){
		String day = Util.getDateTimeStr(System.currentTimeMillis());
		/**2018-01-01**/
		day = day.substring(0, 10);
		if(day.equals(today) == false){
			today = day;
			curFileNo = 0;
		}
		
		File dir = new File("./data/");
		if(!dir.exists()){
			dir.mkdirs();
		}
		
		File outFile = new File("./data/api_" + today + "_" + curFileNo + ".data");
		boolean append = outFile.exists();
		if(outFile.exists()){
			if((lastOpen != 0) && ((System.currentTimeMillis() - lastOpen) >= MAX_FILE_TIME)){
				outFile = new File("./data/" + today + "_" + curFileNo + ".stat");
				append  = false;
				lastOpen = System.currentTimeMillis();
			}
		}
		LOG.debug("Open file name {}", outFile.getName());
		try{
			FileOutputStream fos = new FileOutputStream(outFile, append); 
			return fos;
		}catch(Throwable t){
			LOG.error("Open file {} exceptions", outFile.getName(), t);
		}
		
		return null;
	}
}
