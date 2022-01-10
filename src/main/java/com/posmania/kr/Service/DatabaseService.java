package com.posmania.kr.Service;

import org.springframework.web.socket.WebSocketSession;

public interface DatabaseService {
	
	String searchMaxVersion(WebSocketSession session);
	
}
