package com.posmania.kr.Service;

import java.util.Map;

import org.springframework.web.socket.WebSocketSession;

public interface AddressService {
	
	String searchAddress(Map<String, Object> param, WebSocketSession session);
}
