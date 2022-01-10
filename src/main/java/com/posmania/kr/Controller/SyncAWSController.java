package com.posmania.kr.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.posmania.kr.Service.SyncAWSService;

@Controller
public class SyncAWSController {
	
	@Autowired 
	SyncAWSService synSv;
	
	public void syncAWSStatus(String storeID, int statusCD, String statusMsg) {
		synSv.syncAWSStatus(storeID, statusCD, statusMsg);	
	}
	
	public void syncAWSDMLStatus(Map<String, Object> param) {
		synSv.syncAWSDMLStatus(param);
	}
	
	public void syncAWSMaxID(Map<String, Object> param) {
		synSv.syncAWSMaxID(param);
	}
	
	public void syncAWSApplyID(Map<String, Object> param) {
		synSv.syncAWSApplyID(param);
	}
}