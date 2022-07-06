package com.posmania.kr.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.posmania.kr.Service.DatabaseService;

@Controller
public class DatabaseController {
	
	@Autowired 
	DatabaseService ds;
	
	public String handleAction(WebSocketSession session) {
		
		
		return ds.searchMaxVersion(session);
	}

}