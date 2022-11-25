package com.posmania.kr.Service.Impl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;
import com.posmania.kr.Mapper.AddressMapper;
import com.posmania.kr.Service.AddressService;

@Service
public class AddressServiceImpl implements AddressService {
	
	@Autowired AddressMapper mapper;
	
	Logger logger = LoggerFactory.getLogger(AddressServiceImpl.class);

	public String searchAddress(Map<String, Object> param, WebSocketSession session) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		JsonObject jsonObject = new JsonObject();
		
		try {
			
			result = mapper.searchAddress(param);
			
			int iStatusCD = Integer.parseInt(result.get("StatusCD").toString());
			BigInteger storeID = new BigInteger(param.get("StoreID").toString());
			BigInteger saleID = new BigInteger(param.get("SaleID").toString());
			
			if(iStatusCD == 2) {
				
				jsonObject.addProperty("Result", 0);
				jsonObject.addProperty("StatusCD", Integer.parseInt(result.get("StatusCD").toString()));
				jsonObject.addProperty("StoreID", storeID);
				jsonObject.addProperty("SaleID", saleID);
				jsonObject.addProperty("CustomerName", result.get("CustomerName").toString());
				jsonObject.addProperty("EncNumber", result.get("EncNumber").toString());
				jsonObject.addProperty("ZipCode", result.get("ZipCode").toString());
				jsonObject.addProperty("Addr", result.get("Addr").toString());
				jsonObject.addProperty("AddrDetail", result.get("AddrDetail").toString());
				jsonObject.addProperty("DeliveryMemo", result.get("DeliveryMemo").toString());
				
			} else {
				
				jsonObject.addProperty("Result", 0);
				jsonObject.addProperty("StatusCD", Integer.parseInt(result.get("StatusCD").toString()));
				jsonObject.addProperty("StoreID", storeID);
				jsonObject.addProperty("SaleID", saleID);
			
			}
			
		} catch (Exception e) {
			logger.error("Address Check(207) Error {}", e.getMessage());
			jsonObject.addProperty("Result", 3);
			jsonObject.addProperty("StatusCD", 0);
		}
		
		return jsonObject.toString();
	}
}
