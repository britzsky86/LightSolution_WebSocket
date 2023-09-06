package com.posmania.kr.Service;

import java.util.Map;

import org.springframework.web.socket.WebSocketSession;

public interface TodayResetService {
	
	String todayReset(Map<String, Object> param, WebSocketSession session);
}
