package com.posmania.kr.Mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SyncAWSMapper {

	void syncAWSStatus(Map<String, Object> param);
	void syncAWSDMLStatus(Map<String, Object> param);
	void syncAWSMaxID(Map<String, Object> param);
	void SyncAWSLastID(Map<String, Object> param);
	void syncAWSApplyID(Map<String, Object> param);
}
