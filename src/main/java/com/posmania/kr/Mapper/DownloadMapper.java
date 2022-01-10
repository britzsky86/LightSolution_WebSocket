package com.posmania.kr.Mapper;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface DownloadMapper {

	List<Map<String, Object>> searchDml(Map<String, Object> param);
	BigInteger getMaxSyncID(Map<String, Object> param);
}
