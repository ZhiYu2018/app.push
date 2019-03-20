package push.front.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import push.front.api.pojo.ApiRequest;
import push.front.api.pojo.ApiResponse;
import push.front.api.server.SenderRouter;
import push.front.api.util.Util;


@RestController()
@RequestMapping("/api")
public class Gateway {
	private static Logger logger = LoggerFactory.getLogger(Gateway.class);
	
	@Autowired
	private SenderRouter router;
	
	@RequestMapping(value="/push", method = RequestMethod.POST)
	@ResponseBody
	public ApiResponse push(@RequestBody ApiRequest req){
		ApiResponse rsp = new ApiResponse();
		if((req.getToken_list() == null) || req.getToken_list().isEmpty()
		   || (Util.getMethod(req.getMethod()) == null)){
			logger.info("{} args error", req.getMsgId());
			rsp.setStat(400);
			rsp.setReason("args error");
			return rsp;
		}
		
		rsp = router.router(req);
		return rsp;
	}

}
