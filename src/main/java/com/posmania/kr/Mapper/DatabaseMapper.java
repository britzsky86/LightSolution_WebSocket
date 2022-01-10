package com.posmania.kr.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface DatabaseMapper {

	String searchMaxVersion();
}
