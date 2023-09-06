package com.posmania.kr.Service.Impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;
import com.posmania.kr.Mapper.TodayResetMapper;
import com.posmania.kr.Service.TodayResetService;

@Service
public class TodayResetServiceImpl implements TodayResetService {
	
	@Autowired TodayResetMapper mapper;
	
	Logger logger = LoggerFactory.getLogger(TodayResetServiceImpl.class);
	
	public String todayReset(Map<String, Object> param, WebSocketSession session) {
		// TODO Auto-generated method stub
		
		int result = 0;
		JsonObject jsonObject = new JsonObject();
		
		try {
			result = mapper.todayReset(param);
			
			if (result > 0) {
				jsonObject.addProperty("Result", 0);
				jsonObject.addProperty("MSG", "정상 삭제");
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("Address Check(208) Error {}", e.getMessage());
			jsonObject.addProperty("Result", 3);
			jsonObject.addProperty("MSG", e.getMessage());
		}
		
		return jsonObject.toString();
	}
}
