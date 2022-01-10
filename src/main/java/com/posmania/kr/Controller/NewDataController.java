package com.posmania.kr.Controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.posmania.kr.Service.NewDataService;
import com.posmania.kr.Service.SyncAWSService;

@Controller
public class NewDataController {
	
	@Autowired 
	SyncAWSService syncAWS;
	
	@Autowired
	NewDataService newSrv;
	
	public int handleAction(WebSocketSession session, JsonElement element) {
		
		int iResult = 0;
		
		try {
			
			JsonObject jsonObject = element.getAsJsonObject();
			JsonArray dataArray = jsonObject.get("Data").getAsJsonArray();
			
			BigInteger storeID = new BigInteger(element.getAsJsonObject().get("StoreID").toString());
			int maxSyncID = Integer.parseInt(element.getAsJsonObject().get("MaxSyncID").toString());
			
			List<Map<String, Object>> resultList = new ArrayList<>();
			
			for(int i=0; i<dataArray.size(); i++){
				
				Map<String, Object> param = new HashMap<String, Object>();
				
				JsonObject sqlObject = (JsonObject) dataArray.get(i); 
	            
				String sql = sqlObject.get("Dml").toString().replace("\"", "");
	            String table = sqlObject.get("Table").toString();
	            int awsSyncID = Integer.parseInt(sqlObject.get("SyncID").toString());
	            
	            param.put("DML", sql.replace("\\", "\""));
	            param.put("StoreID", storeID);
	            param.put("AwsSyncID", awsSyncID);
	            param.put("Table", table);
	            
	            resultList.add(param);
			}
			
	        iResult = newSrv.saveData(resultList, session);
	        
	        // 저장에 성공하면 MaxSyncID로 TbSyncAwsST MaxNewAwsSyncID 수정.
	        if (iResult > 0) {
				
				Map<String, Object> params = new HashMap<String, Object>();
				
				params.put("StoreID", storeID);
				params.put("SyncID", maxSyncID);
				
				syncAWS.syncAWSMaxID(params);
	        }
	 
	    } catch (JsonIOException e) {
	        e.printStackTrace();
	    }
		
		return iResult;
	}
}