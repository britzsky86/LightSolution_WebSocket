package com.posmania.kr.Mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface ResetMapper {

	int saveData(Map<String, Object> param);
}
