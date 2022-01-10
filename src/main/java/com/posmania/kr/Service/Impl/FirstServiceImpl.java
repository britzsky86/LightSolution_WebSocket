package com.posmania.kr.Service.Impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.posmania.kr.Mapper.FirstMapper;
import com.posmania.kr.Service.FirstService;

@Service
public class FirstServiceImpl implements FirstService {
	
	@Autowired FirstMapper mapper;

	@Override
	public int saveData(List<Object> params) {
		// TODO Auto-generated method stub
		return mapper.saveData(params);
	}
}
