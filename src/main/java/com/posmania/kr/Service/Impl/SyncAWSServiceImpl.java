package com.posmania.kr.Service.Impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.posmania.kr.Mapper.SyncAWSMapper;
import com.posmania.kr.Service.SyncAWSService;


@Service
public class SyncAWSServiceImpl implements SyncAWSService {
	
	@Autowired SyncAWSMapper mapper;

	@Override
	public void syncAWSStatus(String storeID, int statusCD, String statusMsg) {
		
		Map<String, Object> param = new HashMap<String, Object>();
		
		param.put("StoreID", Long.parseLong(storeID));
		param.put("StatusCD", statusCD);
		param.put("StatusMsg", statusMsg);
		
		mapper.syncAWSStatus(param);
	}
	
	public void syncAWSDMLStatus(Map<String, Object> param) {
		mapper.syncAWSDMLStatus(param);
	}
	
	public void syncAWSMaxID(Map<String, Object> param) {
		mapper.syncAWSMaxID(param);
	}
	
	public void SyncAWSLastID(Map<String, Object> param) {
		mapper.SyncAWSLastID(param);
	}
	
	public void syncAWSApplyID(Map<String, Object> param) {
		mapper.syncAWSApplyID(param);
	}
}
