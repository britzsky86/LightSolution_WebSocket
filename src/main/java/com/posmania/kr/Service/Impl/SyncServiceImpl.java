package com.posmania.kr.Service.Impl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.posmania.kr.Mapper.SyncMapper;
import com.posmania.kr.Service.SyncService;

@Service
public class SyncServiceImpl implements SyncService {
	
	@Autowired SyncMapper mapper;
	
	Logger logger = LoggerFactory.getLogger(SyncServiceImpl.class);
	
	public String searchSyncStatus(Map<String, Object> param) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		JsonObject jsonObject = new JsonObject();
		
		try {
			
			result = mapper.searchSyncStatus(param);
			
			BigInteger lastStoreSyncID = new BigInteger(result.get("LastStoreSyncID").toString());
			BigInteger maxNewAwsSyncID = new BigInteger(result.get("MaxNewAwsSyncID").toString());
			
			jsonObject.addProperty("Result", 0);
			jsonObject.addProperty("StatusCD", Integer.parseInt(result.get("StatusCD").toString()));
			jsonObject.addProperty("LastStoreSyncID", lastStoreSyncID);
			jsonObject.addProperty("MaxNewAwsSyncID", maxNewAwsSyncID);
			jsonObject.addProperty("MSG", result.get("Msg").toString());
			
		} catch (Exception e) {
			logger.error("SyncID Check(204) Error {}", e.getMessage());
			jsonObject.addProperty("Result", 1);
			jsonObject.addProperty("StatusCD", 0);
		}
		
		return jsonObject.toString();
	}
}
