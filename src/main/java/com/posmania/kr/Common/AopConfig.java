package com.posmania.kr.Common;

import java.io.IOException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class AopConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(AopConfig.class);
	
	/*String message;*/
	
	@Before("execution(* com.posmania.kr.Handler.*Handler.*(..))")
	public void doSomethingBefore(JoinPoint joinPoint) {
	}
	
	@After("execution(* com.posmania.kr.Handler.*Handler.*(..))")
	public void doSomethingAfter(JoinPoint joinPoint) {
	}
	
	// return value
	@AfterReturning(pointcut="loggerPointCut()", returning="returnString") 
	public void afterReturningMethod(JoinPoint jp, String returnString) throws Exception { 
		logger.info("afterReturningMethod() returnString : " + returnString);
	}
	
	@Pointcut("execution(* com.posmania.kr.Handler.*Handler.*(..))") // 이런 패턴이 실행될 경우 수행
	public void loggerPointCut() {
	}
	
	// 타겟 메소드에서 예외가 발생한 경우에만 실행된다.
	@AfterThrowing(
			pointcut = "execution(* com.posmania.kr.Service.Impl.UploadServiceImpl.saveData(..))",
			throwing = "ex"
			)
	public void logAfterThrowingV2(JoinPoint joinPoint, Exception ex) {
		
		log.error("ServiceImpl Target Method resulted into exception, message {}", ex.getMessage());
		
		Object[] obj = joinPoint.getArgs();
		
		// 저장할 때, 함께 넘어온 Session 값에 대해 exception을 catch하여 message를 보냄.
		WebSocketSession sessions = (WebSocketSession)obj[1];
		
		try {
			
			JsonObject jsonObj = new JsonObject();
			
			jsonObj.addProperty("Result", 3);
			jsonObj.addProperty("MSG", ex.getMessage());
			
			sessions.sendMessage(new TextMessage(jsonObj.toString()));
			sessions.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.info("ServiceImpl Target Method End");
		
	}
	
	// 타겟 메소드에서 예외가 발생한 경우에만 실행된다.
	@AfterThrowing(
			pointcut = "execution(* com.posmania.kr.Service.Impl.ResetServiceImpl.saveData(..))",
			throwing = "ex"
			)
	public void logAfterThrowingV3(JoinPoint joinPoint, Exception ex) {
		
		log.error("Reset ServiceImpl Target Method resulted into exception, message {}", ex.getMessage());
		
		Object[] obj = joinPoint.getArgs();
		
		// 저장할 때, 함께 넘어온 Session 값에 대해 exception을 catch하여 message를 보냄.
		WebSocketSession sessions = (WebSocketSession)obj[1];
		
		try {
			
			JsonObject jsonObj = new JsonObject();
			
			jsonObj.addProperty("Result", 3);
			jsonObj.addProperty("MSG", ex.getMessage());
			
			sessions.sendMessage(new TextMessage(jsonObj.toString()));;
			sessions.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.info("Reset ServiceImpl Target Method End");
		
	}
}