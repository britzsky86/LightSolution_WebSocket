package com.posmania.kr.Controller;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonElement;
import com.posmania.kr.Service.DownloadService;



@Controller
public class DownloadController {
	
	@Autowired 
	DownloadService ds;
	
	public String handleAction(WebSocketSession session, JsonElement element) {
		
		BigInteger appliAwsID = BigInteger.valueOf(element.getAsJsonObject().get("AppliedAwsSyncID").getAsLong());
		BigInteger storeID = BigInteger.valueOf(element.getAsJsonObject().get("StoreID").getAsLong());
		
		Map<String, Object> param = new HashMap<String, Object>();
		
		param.put("AppliedAwsSyncID", appliAwsID);
		param.put("StoreID", storeID);
		
		return ds.searchDml(param, session);
	}
}