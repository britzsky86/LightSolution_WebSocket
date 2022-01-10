package com.posmania.kr.Controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.posmania.kr.Service.SyncAWSService;
import com.posmania.kr.Service.UploadService;

@Controller
public class UploadController {
	
	@Autowired 
	UploadService ups;
	
	@Autowired 
	SyncAWSService syncAWS;
	
	Logger logger = LoggerFactory.getLogger(UploadController.class);
	
	public String handleAction(WebSocketSession session, JsonElement element) {
		
		String result = "";
		
		try {
			
			BigInteger storeID = new BigInteger(element.getAsJsonObject().get("StoreID").toString());
			BigInteger syncID = new BigInteger(element.getAsJsonObject().get("LastSyncID").toString());
				
			JsonObject jsonObject = element.getAsJsonObject();
			JsonArray dataArray = jsonObject.get("Data").getAsJsonArray();
			
			List<Object> sqlsList = new ArrayList<Object>();
			
			for(int i=0; i<dataArray.size(); i++){
				
				JsonObject sqlObject = (JsonObject) dataArray.get(i); 
				
				String Sql = sqlObject.get("Sql").getAsString();
				
	            Sql = Sql.replaceAll("\\s", " ");
	            
	            sqlsList.add(Sql);
			}
			
			result = ups.saveData(sqlsList, session, storeID);
	        
	        // 저장에 성공하면 LastSyncID를 TbSyncAwsST LastStoreSyncID 에 저장.
	        if (!result.isEmpty()) {
	        	
				Map<String, Object> param = new HashMap<String, Object>();
				
				param.put("StoreID", storeID);
				param.put("SyncID", syncID);
				
				syncAWS.SyncAWSLastID(param);
	        }
			
		} catch (Exception e) {
			
			logger.info("Upload(201) Error {}", e.getMessage());
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("result", 0);
			
			result = jsonObject.toString();
		
		}
		
		return result;
	}
}