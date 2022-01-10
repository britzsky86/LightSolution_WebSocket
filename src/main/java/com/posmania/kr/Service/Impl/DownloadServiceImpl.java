package com.posmania.kr.Service.Impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.posmania.kr.Mapper.DownloadMapper;
import com.posmania.kr.Service.DownloadService;

@Service
public class DownloadServiceImpl implements DownloadService {
	
	@Autowired DownloadMapper mapper;

	public String searchDml(Map<String, Object> param, WebSocketSession session) {
		
		BigInteger maxSyncID = null;
		List<Map<String, Object>> resultList = new ArrayList<>();
		
		maxSyncID = mapper.getMaxSyncID(param);
		resultList = mapper.searchDml(param);
		
		JsonObject jsonObject = new JsonObject();
		JsonParser parser = new JsonParser();
		JsonArray dataArr = new JsonArray();
		
		if(resultList.size() > 0) {
			
			jsonObject.addProperty("MaxSyncID", maxSyncID);
			
			for(int i=0; i<resultList.size(); i++) {
				
				JsonObject data = new JsonObject();
				Object obj = parser.parse(resultList.get(i).get("SyncDML").toString());
				data = (JsonObject)obj;
				
				dataArr.add(data);
			}
			
			jsonObject.add("Data", dataArr);
			
		} else {
			
			// 전달할 데이터가 없으면 조회조건으로 받았던 AppliedAwsSyncID 를 그대로 다시 전달한다.
			BigInteger syncID = new BigInteger(param.get("AppliedAwsSyncID").toString());
			
			jsonObject.addProperty("MaxSyncID", syncID);
		}
		
		return jsonObject.toString();
	}
}
