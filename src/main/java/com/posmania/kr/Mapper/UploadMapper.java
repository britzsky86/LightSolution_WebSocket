package com.posmania.kr.Mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface UploadMapper {

	int saveData(List<Object> params);
}
