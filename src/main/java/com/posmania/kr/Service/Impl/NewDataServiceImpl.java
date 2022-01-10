package com.posmania.kr.Service.Impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.posmania.kr.Mapper.NewDataMapper;
import com.posmania.kr.Service.NewDataService;

@Service
public class NewDataServiceImpl implements NewDataService {
	
	@Autowired NewDataMapper mapper;

	@Override
	public int saveData(List<Map<String, Object>> list, WebSocketSession session) {
		
		
		int cnt = 0;
		
		for(Map<String, Object> dto : list) {
			cnt++;
			mapper.saveData(dto);
		}
		
		return cnt;
	}
}
