package com.posmania.kr.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.posmania.kr.Service.FirstService;

@Controller
public class FirstController {
	
	@Autowired 
	FirstService fs;
	
	public void handleAction(WebSocketSession session, List<Object> params) {
		
		int iResult = fs.saveData(params);
		
		System.out.println("result :: " + iResult);
	}

}