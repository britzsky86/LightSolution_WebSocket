package com.posmania.kr.Service;

import java.util.Map;

import org.springframework.web.socket.WebSocketSession;

public interface ResetService {
	
	String saveData(WebSocketSession session, Map<String, Object> param);
	
}
