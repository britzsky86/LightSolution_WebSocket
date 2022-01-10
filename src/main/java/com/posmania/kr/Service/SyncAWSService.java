package com.posmania.kr.Service;

import java.util.Map;

public interface SyncAWSService {
	
	void syncAWSStatus(String storeID, int statusCD, String statusMsg);
	void syncAWSDMLStatus(Map<String, Object> param);
	void syncAWSMaxID(Map<String, Object> param);
	void SyncAWSLastID(Map<String, Object> param);
	void syncAWSApplyID(Map<String, Object> param);
}
