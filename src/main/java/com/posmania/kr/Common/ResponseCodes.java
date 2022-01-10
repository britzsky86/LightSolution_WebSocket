package com.posmania.kr.Common;

public enum ResponseCodes {
	
	SUCCESS("0","성공"),
	FAIL_DATABASE("1","데이터베이스 에러"),
	FAIL_NETWORK("2","송수신 에러"),
	FAIL_WEBSOCKET("3","웹소켓 에러"),
	FAIL_SERVERLOAD("4","서버 과부하"),
	FAIL_TIMEOUT("5","서버타임아웃");
	
	private final String Code; 
    private final String Message;
	
    private ResponseCodes(String code, String message) {
		Code = code;
		Message = message;
	}

	public String getCode() {
		return Code;
	}

	public String getMessage() {
		return Message;
	}

}
