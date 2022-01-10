package com.posmania.kr.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.posmania.kr.Service.FileService;

@Controller
public class FileController {
	
	@Autowired 
	FileService fs;
	
	public int handleAction(WebSocketSession session,  List<Object> params) {
		
		int iResult = fs.saveData(params);
		
		return iResult;
	}
}