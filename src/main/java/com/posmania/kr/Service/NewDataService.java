package com.posmania.kr.Service;

import java.util.List;
import java.util.Map;

import org.springframework.web.socket.WebSocketSession;

public interface NewDataService {
	
	int saveData(List<Map<String, Object>> list, WebSocketSession session);
	
}
