package com.posmania.kr.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.posmania.kr.Service.ResetService;



@Controller
public class ResetController {
	
	@Autowired 
	ResetService reset;
	
	public String handleAction(WebSocketSession session, Map<String, Object> param) {
		
		return reset.saveData(session, param);
	}

}