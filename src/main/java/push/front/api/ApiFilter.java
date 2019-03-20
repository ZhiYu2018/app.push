package push.front.api;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiFilter extends OncePerRequestFilter implements Filter{
	private static Logger logger = LoggerFactory.getLogger(ApiFilter.class);
	private final static String TID = "tid";
	private final static AtomicLong SEQ = new AtomicLong(0);

	@Override
	protected void doFilterInternal(HttpServletRequest req,
			HttpServletResponse rsp, FilterChain fc)
			throws ServletException, IOException {
		MDC.put(TID, "SEQ." + String.valueOf(SEQ.incrementAndGet()));
		String ip = req.getRemoteAddr();
		int    port = req.getRemotePort();
		long start = System.currentTimeMillis();
		try{
			fc.doFilter(req, rsp); 
		}catch(ServletException e){
			e.printStackTrace();
			throw e;
		}catch(IOException t){
			t.printStackTrace();
			throw t;
		}finally{
			long end = System.currentTimeMillis() - start;
			if(end <= 200){
				logger.info("Handle [{}:{}] request over, time used:{} ms", ip, port, end);
			}else{
				logger.warn("Handle [{}:{}] request over, time used:{} ms Slowly", ip, port, end);
			}
			MDC.clear();
		}		
		
	}
}
