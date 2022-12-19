package com.posmania.kr.Handler;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.posmania.kr.Common.ResponseCodes;
import com.posmania.kr.Controller.AddressController;
import com.posmania.kr.Controller.DatabaseController;
import com.posmania.kr.Controller.DownloadController;
import com.posmania.kr.Controller.NewDataController;
import com.posmania.kr.Controller.ResetController;
import com.posmania.kr.Controller.SyncAWSController;
import com.posmania.kr.Controller.SyncController;
import com.posmania.kr.Controller.UploadController;

enum RequestType { 
	
	// UPLOAD(201) 		저장데이터 업로드(#20835)
	// DOWNLOAD(202)	신규 데이터 다운로드(#20836)
	// DATABASE(203) 	데이터베이스 버전 확인(#20837)
	// SYNCID(204) 		동기화ID 확인(#20850)
	// RESET(205) 		데이터 리셋 (#20851)
	// NEW(206) 		데이터 생성
	// ADDRESS			배달주소 확인 (#24409)
	// ADDRESS_RESET	배달주소 리셋 (#24695
	UPLOAD(201), DOWNLOAD(202), DATABASE(203), SYNCID(204), RESET(205), NEW(206), ADDRESS(207), ADDRESS_RESET(208); 
	
	private final int value;
	
	RequestType(int value) { this.value = value; }
	
    public int getValue() { return value; }
}

@EnableAspectJAutoProxy
@Component
public class WebSocketHandler extends TextWebSocketHandler {

	/*private static final List<WebSocketSession> list = new ArrayList<>();*/
	// ConcurrentModificationException 에러를 해결하고자 CopyOnWriteArrayList 로 세션을 관리함.
	// ConcurrentModificationException 은 for loop 에서 서로 다른 쓰레드가 List에 간섭하고 데이터 조작을 할 때, 발생한다고 알려져 있음.
	// 보통 콜렉션 객체에서는 IndexOutOfBoundException 이 발생하지만 ConcurrentModificationException 는 for loop 동작방식과 밀접한 관련이 있음.
	// 참고 : https://m.blog.naver.com/tmondev/220393974518
	private final List<WebSocketSession> lists = new CopyOnWriteArrayList<>();
	
	@Autowired
	private UploadController upload;	// 신규데이터 업로드.
	
	@Autowired
	private DownloadController down;	// 신규데이터 다운로드.
	
	@Autowired
	private DatabaseController db;		// DB버전 체크.
	
	@Autowired
	private SyncAWSController syncAWS;	// Sync 상태 변경.
	
	@Autowired
	private SyncController sync;		// 동기화 상태 체크.
	
	@Autowired
	private NewDataController newCtr;	// Web ASP에서 생성된 신규 데이터.
	
	@Autowired
	private ResetController reset;		// 매장별 리셋.
	
	@Autowired
	private AddressController address;	// 배달주소 체크.
	
	Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
	
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		
		for(WebSocketSession sess : lists) {
			
			if(sess.getId().equals(session.getId())) {
			
				String payload = message.getPayload().toString();
				
				JsonParser parser = new JsonParser();
				JsonElement element = parser.parse(payload);
				
				logger.info("session info {}", element);
				
				int iType = element.getAsJsonObject().get("Type").getAsInt();
				int iResult = 0;
				String result = "";
				
				JsonObject jsonObj = new JsonObject();
				
				if(RequestType.UPLOAD.getValue() == iType){
					
					logger.info("Upload(201) Start");
					
					result = upload.handleAction(session, element);
					
					if (!result.isEmpty()) {
						
						logger.info("Upload(201) Result {}", result);
						sess.sendMessage(new TextMessage(result));
						
						// 업로드 후 바로 다운로드를 시도하기 때문에 세션을 닫지 않는다.
						/*sess.wait(1000);
						sess.close();
						lists.remove(sess);*/
					} 
					
				} else if(RequestType.DOWNLOAD.getValue() == iType){
					
					logger.info("Down(202) Start");
					
					result = down.handleAction(session, element);
					
					BigInteger appliedAwsSyncID = BigInteger.valueOf(element.getAsJsonObject().get("AppliedAwsSyncID").getAsLong());
					BigInteger storeID = BigInteger.valueOf(element.getAsJsonObject().get("StoreID").getAsLong());
					
					// 조회된 MaxSyncID를 추출하기 위해 Map형식으로 Convert.
					Map<String, Object> dataMap = new HashMap<String, Object>();
					dataMap = convertJSONstringToMap(result);
					
					BigInteger maxSyncID = new BigInteger(dataMap.get("MaxSyncID").toString());
					
					message = new TextMessage(result);
					
					// 각종 업데이트를 위해 필요한 조건 세팅.
					Map<String, Object> param = new HashMap<String, Object>();
					Map<String, Object> params = new HashMap<String, Object>();
					
					param.put("StoreID", storeID);
					param.put("AppliedAwsSyncID", appliedAwsSyncID);
					param.put("MaxSyncID", maxSyncID);
					
					params.put("StoreID", storeID);
					
					try {
						// 전송을 시작하면 TbSyncAwsDML의 StatusCD를 처리중(1)로 업데이트.
						param.put("StatusCD", 1);
						syncAWS.syncAWSDMLStatus(param);
						
						
						logger.info("Down(202) Result {}", message);
						// 전송.
						sess.sendMessage(message);
						/*sess.close();*/
						/*lists.remove(sess);*/
						
						logger.info("Down(202) End");
						
						// 전송에 성공하면 TbSyncAwsDML의 StatusCD를 완료(0)로 업데이트.
						param.put("StatusCD", 0);
						syncAWS.syncAWSDMLStatus(param);
						
						// TbSyncAwsST의 MaxNewAwsSyncID를 MaxSyncID로 업데이트.
						params.put("SyncID", maxSyncID);
						syncAWS.syncAWSApplyID(params);
						
					} catch (Exception e) {
						
						logger.error("Down(202) Error {} ", e.getMessage());
						
						// 실패하면 처리중이었던 StatusCD를 미전송(2)로 업데이트.
						param.put("StatusCD", 2);
						syncAWS.syncAWSDMLStatus(param);
						
					}
					
				} else if(RequestType.DATABASE.getValue() == iType){
					
					logger.info("Version Check(203) Start");
					
					String version = db.handleAction(session);
					
					logger.info("Version Check(203) Result {}", version);
					
					sess.sendMessage(new TextMessage(version));
					/*sess.close();*/
					/*lists.remove(sess);*/
					
					logger.info("Version Check(203) End");
					
				} else if(RequestType.SYNCID.getValue() == iType){
					
					logger.info("SyncID Check(204) Start");
					
					BigInteger storeID = BigInteger.valueOf(element.getAsJsonObject().get("StoreID").getAsLong());
					
					Map<String, Object> param = new HashMap<String, Object>();
					
					param.put("StoreID", storeID);
					
					JsonObject jsonObject = new JsonObject();
					
					String resultStr = sync.handleAction(session, param);
					
					logger.info("SyncID Check(204) Result {}", resultStr);
					
					sess.sendMessage(new TextMessage(resultStr));
					/*sess.close();*/
					/*lists.remove(sess);*/
					
					logger.info("SyncID Check(204) End");
					
				} else if(RequestType.RESET.getValue() == iType){
					
					logger.info("Reset(205) Start");
					
					BigInteger storeID = BigInteger.valueOf(element.getAsJsonObject().get("StoreID").getAsLong());
					
					Map<String, Object> param = new HashMap<String, Object>();
					
					param.put("StoreID", storeID);	
					
					String resetResult = "";
					
					try {
						// Reset을 시작한다.
						syncAWS.syncAWSStatus(element.getAsJsonObject().get("StoreID").toString(), 1, "Reset 시작");
						
						resetResult = reset.handleAction(session, param);
						
						// Reset을 종료한다.
						syncAWS.syncAWSStatus(element.getAsJsonObject().get("StoreID").toString(), 0, "Reset 종료");
						
						logger.info("Reset(205) Result {}", resetResult);
						sess.sendMessage(new TextMessage(resetResult));
						/*sess.close();*/
						/*lists.remove(sess);*/
						
						logger.info("Reset(205) End");
						
					} catch (Exception e) {
						
						// Reset을 실패.
						logger.error("Reset(205) Error {} ", e.getMessage());
						syncAWS.syncAWSStatus(element.getAsJsonObject().get("StoreID").toString(), 3, "Reset 실패");
						sess.sendMessage(new TextMessage(resetResult));
						/*sess.close();*/
						/*lists.remove(sess);*/
					}
					
				} else if(RequestType.NEW.getValue() == iType) {
					
					logger.info("NewData(206) Start");
					
					iResult = newCtr.handleAction(session, element);
					
					if (iResult > 0) {
						
						jsonObj.addProperty("Result", 0);
						jsonObj.addProperty("MSG", "성공");
						
						sess.sendMessage(new TextMessage(jsonObj.toString()));
						/*sess.close();*/
						/*lists.remove(sess);*/
						
						logger.info("NewData(206) End");
						
					} else {
						
						jsonObj.addProperty("Result", "400");
						jsonObj.addProperty("MSG", "실패");
						
						logger.error("NewData(206) Error {} ", element);
						sess.sendMessage(new TextMessage(jsonObj.toString()));
						/*sess.close();*/
						/*lists.remove(sess);*/
					}
					
				} else if(RequestType.ADDRESS.getValue() == iType) {
					
					logger.info("AddressData(207) Start");
					
					result = address.handleAction(session, element);
					
					message = new TextMessage(result);
					
					try {
						
						logger.info("AddressData(207) Result {}", message);
						// 전송.
						sess.sendMessage(message);
						/*sess.close();*/
						/*lists.remove(sess);*/
						
						logger.info("AddressData(207) End");
						
					} catch (Exception e) {
						
						logger.error("AddressData(207) Error {} ", e.getMessage());
						
					}
				} else if(RequestType.ADDRESS_RESET.getValue() == iType) {
					
					logger.info("AddressRestData(208) Start");
					
					result = address.resetHandleAction(session, element);
					
					message = new TextMessage(result);
					
					try {
						
						logger.info("AddressRestData(208) Result {}", message);
						// 전송.
						sess.sendMessage(message);
						/*sess.close();*/
						/*lists.remove(sess);*/
						
						logger.info("AddressRestData(208) End");
						
					} catch (Exception e) {
						
						logger.error("AddressRestData(208) Error {} ", e.getMessage());
						
					}
				}
			}
		}	
	}
	
	public static Map<String,Object> convertJSONstringToMap(String json) throws Exception {
        
		ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = new HashMap<String, Object>();
        
        map = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        
        return map;
    }
	
	// 클라이언트가 접속 시 호출
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
	   
		lists.add(session);
		/*log.info(session + " 클라이언트 접속");*/
		logger.info("Session Connect Open");
	   
	}
	
	// 클라이언트가 접속 해제 시 호출
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		
		/*log.info(session + " 클라이언트 접속 해제");*/
		logger.info("Session Connect Close");
		lists.remove(session);
	   
	}
	
	// 클라이언트가 데이터 송수신 중에 에러가 발생할 경우 호출
	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		
		// 네트워크 에러 처리
		// Result 2
		JsonObject jsonObj = new JsonObject();
		
		jsonObj.addProperty("Result", ResponseCodes.FAIL_NETWORK.getCode());
		jsonObj.addProperty("MSG", ResponseCodes.FAIL_NETWORK.getMessage());
		
		for(WebSocketSession sess : lists) {
			
			if(sess.getId().equals(session.getId())) {
				
				sess.sendMessage(new TextMessage(jsonObj.toString()));
			}
		}
		
		logger.error("DataTransportError {}", exception.getMessage());
		logger.error("DataTransportError {}", jsonObj.toString());
		
		lists.remove(session);
	   
	}
}
