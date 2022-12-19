package com.posmania.kr.Mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface AddressMapper {

	Map<String, Object> searchAddress(Map<String, Object> param);
	int resetAddress(Map<String, Object> param);
}
