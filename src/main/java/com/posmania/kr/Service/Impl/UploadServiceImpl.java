package com.posmania.kr.Service.Impl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;
import com.posmania.kr.Mapper.DownloadMapper;
import com.posmania.kr.Mapper.UploadMapper;
import com.posmania.kr.Service.UploadService;

@Service
public class UploadServiceImpl implements UploadService {
	
	@Autowired UploadMapper mapper;
	
	@Autowired DownloadMapper downMapper;
	
	@Override
	public String saveData(List<Object> params, WebSocketSession session, BigInteger storeID) {
		
		int result = 0;
		
		result = mapper.saveData(params);
		
		JsonObject jsonObject = new JsonObject();
		
		if(result > 0) {
			
			Map<String, Object> param = new HashMap<String, Object>();
			
			param.put("StoreID", storeID);
			
			BigInteger maxSyncID = downMapper.getMaxSyncID(param);
			
			jsonObject.addProperty("result", 0);
			jsonObject.addProperty("MaxSyncID", maxSyncID);
		}
		
		return jsonObject.toString();
	}
}
