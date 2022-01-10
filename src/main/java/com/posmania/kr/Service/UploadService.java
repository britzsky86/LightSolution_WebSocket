package com.posmania.kr.Service;

import java.math.BigInteger;
import java.util.List;

import org.springframework.web.socket.WebSocketSession;

public interface UploadService {
	
	String saveData(List<Object> params, WebSocketSession session, BigInteger storeID);
	
}
