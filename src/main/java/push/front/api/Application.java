package push.front.api;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.DispatcherType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import push.front.api.server.ApiContext;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class Application 
{
	private static Logger logger = LoggerFactory.getLogger(Application.class);
    public static void main( String[] args )
    {
    	logger.info("Hello push!");
        System.out.println( "Hello World!" );
        SpringApplication.run(Application.class, args);
    }
    
    @Bean
    public FilterRegistrationBean<ApiFilter>  filterRegistrationBean() {
    	FilterRegistrationBean<ApiFilter> registrationBean = new FilterRegistrationBean<ApiFilter>(); 
    	ApiFilter filter = new ApiFilter();
    	registrationBean.setFilter(filter);  
    	registrationBean.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
    	List<String> urlPatterns = new ArrayList<String>(); 
    	urlPatterns.add("/api/*");
        registrationBean.setUrlPatterns(urlPatterns); 
    	return registrationBean;
    }
    
    @PostConstruct
    public void init(){
    	logger.info("Post construct ......");
    }
    
    @PreDestroy  
    public void  dostory(){
    	logger.info("Post dostory ......");
    	ApiContext.setQuit();
    }
}
