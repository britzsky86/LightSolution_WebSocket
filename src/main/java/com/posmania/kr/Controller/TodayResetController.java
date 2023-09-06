package com.posmania.kr.Controller;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonElement;
import com.posmania.kr.Service.TodayResetService;

@Controller
public class TodayResetController {
	
	@Autowired
	TodayResetService as;
	
	public String resetHandleAction(WebSocketSession session, JsonElement element) {
		
		BigInteger storeID = BigInteger.valueOf(element.getAsJsonObject().get("StoreID").getAsLong());
		//BigInteger saleID = BigInteger.valueOf(element.getAsJsonObject().get("SaleID").getAsLong());
		
		Map<String, Object> param = new HashMap<String, Object>();
		
		param.put("StoreID", storeID);
		//param.put("SaleID", saleID);
		
		return as.todayReset(param, session);
	}
}