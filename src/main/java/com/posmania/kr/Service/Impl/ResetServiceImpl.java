package com.posmania.kr.Service.Impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;
import com.posmania.kr.Mapper.ResetMapper;
import com.posmania.kr.Service.ResetService;

@Service
public class ResetServiceImpl implements ResetService {
	
	@Autowired ResetMapper mapper;

	@Override
	public String saveData(WebSocketSession session, Map<String, Object> param) {
		
		int result = 0;
		
		JsonObject jsonObject = new JsonObject();
		
		try {
			
			result = mapper.saveData(param);
			
			if(result > 0) {
				
				jsonObject.addProperty("Result", 0);
				jsonObject.addProperty("MSG", "성공");
				
			}
			
		} catch (Exception e) {
			
			// TODO: handle exception
			jsonObject.addProperty("Result", 1);
			jsonObject.addProperty("MSG", e.toString());
		
		}
		
		return jsonObject.toString();
	}
}
