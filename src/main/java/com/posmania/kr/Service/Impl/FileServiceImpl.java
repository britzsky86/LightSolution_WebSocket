package com.posmania.kr.Service.Impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.posmania.kr.Mapper.FileMapper;
import com.posmania.kr.Service.FileService;


@Service
public class FileServiceImpl implements FileService {
	
	@Autowired FileMapper mapper;

	@Override
	public int saveData(List<Object> params) {
		// TODO Auto-generated method stub
		return mapper.saveData(params);
	}
}
