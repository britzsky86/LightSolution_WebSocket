package com.posmania.kr.Service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;
import com.posmania.kr.Mapper.DatabaseMapper;
import com.posmania.kr.Service.DatabaseService;

@Service
public class DatabaseServiceImpl implements DatabaseService {
	
	@Autowired DatabaseMapper mapper;

	@Override
	public String searchMaxVersion(WebSocketSession session) {
		
		JsonObject jsonObject = new JsonObject();
		
		jsonObject.addProperty("Result", 0);
		jsonObject.addProperty("PosManiaVersion", mapper.searchMaxVersion());
		
		return jsonObject.toString();
	}
}
