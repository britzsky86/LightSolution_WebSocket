package com.posmania.kr.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.posmania.kr.Service.SyncService;

@Controller
public class SyncController {
	
	@Autowired 
	SyncService ss;
	
	public String handleAction(WebSocketSession session, Map<String, Object> param) {
		
		String result;
		
		result = ss.searchSyncStatus(param);
		
		return result;
	}

}