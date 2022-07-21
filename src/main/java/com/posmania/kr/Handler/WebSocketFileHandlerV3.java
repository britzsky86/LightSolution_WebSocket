package com.posmania.kr.Handler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.posmania.kr.Common.ResponseCodes;
import com.posmania.kr.Controller.SyncAWSController;

/** 
* @packageName : com.posmania.kr.Handler 
* @fileName : WebSocketFileHandlerV3.java 
* @author : son
* @date : 2021.09.08 
* @description : 
* =========================================================== 
* DATE AUTHOR NOTE 
* ----------------------------------------------------------- 
* 2021.09.08 son 최초 생성 
*/
/*@EnableAsync*/
@Component
public class WebSocketFileHandlerV3 extends TextWebSocketHandler {
	
	@Autowired
	private SyncAWSController syncAWS;
	
	/*private static final List<WebSocketSession> list = new ArrayList<>();*/
	// ConcurrentModificationException 에러를 해결하고자 CopyOnWriteArrayList 로 세션을 관리함.
	// ConcurrentModificationException 은 for loop 에서 서로 다른 쓰레드가 List에 간섭하고 데이터 조작을 할 때, 발생한다고 알려져 있음.
	// 보통 콜렉션 객체에서는 IndexOutOfBoundException 이 발생하지만 ConcurrentModificationException 는 for loop 동작방식과 밀접한 관련이 있음.
	// 참고 : https://m.blog.naver.com/tmondev/220393974518
	private static final List<WebSocketSession> lists = new CopyOnWriteArrayList<>();
	
	BufferedOutputStream bos;
	
	String fileName = "";
	
	Logger logger = LoggerFactory.getLogger(WebSocketFileHandlerV3.class);
    
    @Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    	
    	/*System.out.println("TextHandler Session :: " + session);*/
    	
    	logger.info("FileHandler session info {}", session);
    	
    	String msg = message.getPayload(); //JSON형태의 String메시지를 받는다.
		
		JsonParser parser = new JsonParser();
		JsonElement element = parser.parse(msg);
		
		fileName = element.getAsJsonObject().get("FileName").getAsString();
	}
    
    /*@Async*/
	@Override
	public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
			
		for(WebSocketSession sess : lists) {
			
			if(sess.getId().equals(session.getId())) {
				
				ByteBuffer byteBuffer = message.getPayload();
				
				String firstPath = "/usr/share/output";
				
				boolean bResult = false;
				
				List<String> errorList = new ArrayList<String>();
				
				if(createFolder(firstPath+"/")) {
					
					String folderPathV2 = firstPath+"/"+fileName+"/";
					
					File file = new File(folderPathV2+fileName);
					
					// 압축파일 생성.
					if(create7Zip(byteBuffer, file)) 
						bResult = true;
					else
						bResult = false;
					
				} else {
					
					try {
						
						logger.error("File size 0byte StoreID :: {} ", fileName);
						sess.close();
			        	lists.remove(sess);
			        	
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
				
				// 폴더 및 압축파일 생성이 모두 정상일 때, DB에 적용.
				if(bResult) {
					
					List<String> fileList = new ArrayList<String>();
					
			        /*String folderPathV3 = firstPath+"\\"+fileName+"\\";*/
					
					String folderPathV3 = firstPath+"/"+fileName+"/";
			        
			        try {
						
			        	fileList = un7Zip(folderPathV3+fileName, folderPathV3);
			        	
			        	sendResponse(sess, "");
			        	sess.close();
			        	lists.remove(sess);
			        	
			        	
			        } catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						try {
							
							sendResponse(sess, e.toString());
							sess.close();
							lists.remove(sess);
							
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
			        
			        String storeID = fileName.replace(".7z", "");
			        
			        for(int j=0; j<fileList.size();j++) {
			        	
			        	String sqlFileName = fileList.get(j).toString();
			        	String sqlFilePath = folderPathV3+sqlFileName;
			        	
			        	boolean bSave = true;
				    	Connection con = null;
				    	BufferedReader fr = null;
				    	
				    	try {
				    		
				    		Class.forName("com.mysql.cj.jdbc.Driver").newInstance();

				    		/*con = DriverManager.getConnection("jdbc:mysql://222.98.245.65:3306/PmKioskAWS?allowMultiQueries=true&useSSL=false&autoReconnect=true&characterEncoding=utf8&serverTimezone=UTC","root", "pass_posmania4906!");*/
				    		con = DriverManager.getConnection("jdbc:mysql://alphacashier-asp-db-instance.ctnru7tj6gzl.ap-northeast-2.rds.amazonaws.com:4406/PmKioskAWS?allowMultiQueries=true&useSSL=false&autoReconnect=true&characterEncoding=utf8&serverTimezone=UTC","admin", "pass_softlogic");
				    		con.createStatement();
				    		
				    		// UPDATE TbSyncAwsST SET StatusCD = 2 WHERE StoreID = FileName 로 저장해야 함. (업로드중)
				    		// 최초등록이기 때문에 insert 해야 함.
				    		syncAWS.syncAWSStatus(storeID, 2, null);
				    		
				    		ScriptRunner scriptExecutor = new ScriptRunner(con);
				    		
				    		fr = new BufferedReader(new InputStreamReader(new FileInputStream(sqlFilePath),"UTF-8"));
				    		
				    		StringWriter stringWriter = new StringWriter();
				    		
				    		scriptExecutor.setLogWriter(null);
				    		scriptExecutor.setErrorLogWriter(new PrintWriter(stringWriter));
				    		scriptExecutor.setAutoCommit(true);
				    		scriptExecutor.setStopOnError(true);
				    		
				    		if (!stringWriter.toString().isEmpty()) {
				    			logger.error("scriptExecutor Error Start");
					    		logger.error("scriptExecutor Msg {}", stringWriter.toString());
					    		logger.error("scriptExecutor Error End");
				    		}
				    		
				    		scriptExecutor.runScript(fr);
				    		
				    	} catch (Exception e) {
				    		
				    		String strErrMsg = "";
				    		
				    		bSave = false;
				    		/*iErrorCnt++;*/
				    		
				    		errorList.add(e.getMessage().toString());
				    		
				    		logger.error("errorList Msg {}", errorList.toString());
				    		
				    		if (errorList.isEmpty()) {
				    			// UPDATE TbSyncAwsST SET StatusCD = 3 WHERE StoreID = FileName 로 저장해야 함. (에러발생) 
					    		syncAWS.syncAWSStatus(storeID, 3, strErrMsg);
				    		} else {
				    			// UPDATE TbSyncAwsST SET StatusCD = 3 WHERE StoreID = FileName 로 저장해야 함. (에러발생) 
					    		syncAWS.syncAWSStatus(storeID, 3, errorList.toString());
				    		}
				    	
				    	} finally {
				    		
				    		if(fr != null) {
				    			try {
									fr.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
				    		}
				    		
				    		if(con != null) {
				    			try {
									con.close();
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
				    		}
				    	}
				    	
				    	if(bSave) {
				    		// UPDATE TbSyncAwsST SET StatusCD = 4 WHERE StoreID = FileName 로 저장해야 함.	
				    		syncAWS.syncAWSStatus(storeID, 4, null);
				    	}
				    	
				    	if(deleteDirectoryAndFiles(folderPathV3)) {	
				    		logger.error("Directory Delete Success StoreID :: ", storeID);
				    	}
			        }
			        
				} else {
					logger.error("File size 0byte StoreID :: {} ", fileName);
				}	
			}
		}
	}

	public static int byte2uShort(byte[] bytes, int pos) {   

		int i1 = (bytes[pos]	& 0xFF);
		int i2 = (bytes[pos+1]	& 0xFF);
		
		return ((i1 << 8) + i2);
	}
	
	public void sendResponse(WebSocketSession s, String errorMsg) throws IOException {
		
		JsonObject jsonObj = new JsonObject();
		
		jsonObj.addProperty("Result", 0);
		
		if(errorMsg != "") {
			jsonObj.addProperty("Result", 1);
			jsonObj.addProperty("MSG", errorMsg);
		}
		
		s.sendMessage(new TextMessage(jsonObj.toString()));
		
	}
	
	/**
	  * @Method Name : createFolder
	  * @작성일 : 2021. 9. 8.
	  * @작성자 : KYOUNGWON
	  * @변경이력 : 
	  * @Method 설명 : 전달받은 파일명으로 폴더생성
	  */
	public boolean createFolder(String path) {
		
		logger.info("CreateFolder path {} ", path.toString());
		
		File newFile = new File(path+""+fileName); 
		
		boolean bResult = false;
		
		if(newFile.length() == 0)
			return bResult;
		
		if (!newFile.exists()) {
			
			try {
			    
				bResult = newFile.mkdir(); //폴더 생성합니다.
				Runtime.getRuntime().exec("chmod 777 " + path+""+fileName); 
				newFile.setExecutable(true, false); 
				newFile.setReadable(true, false); 
				newFile.setWritable(true, false); 
				newFile.createNewFile();
			    
			} catch(Exception e) {
			    
				logger.error("CreateFolder error path {} ", e.getMessage());
				bResult = false;
			}        
		}else {
			
			bResult = false;
		}
		
		return bResult;
	}
	
	/**
	  * @Method Name : create7Zip
	  * @작성일 : 2021. 9. 8.
	  * @작성자 : KYOUNGWON
	  * @변경이력 : 
	  * @Method 설명 : 7Zip으로 파일생성
	  */
	public boolean create7Zip(ByteBuffer byteBuffer, File file) {
		
		boolean bResult = false;
		
		FileOutputStream out = null;
		FileChannel outChannel = null;
		
		try {
			
			byteBuffer.flip(); //byteBuffer를 읽기 위해 세팅
			out = new FileOutputStream(file, true); //생성을 위해 OutputStream을 연다.
			outChannel = out.getChannel(); //채널을 열고
			byteBuffer.compact(); //파일을 복사한다.
			outChannel.write(byteBuffer); //파일을 쓴다.
			bResult = true;
			
		}catch(Exception e) {
			e.printStackTrace();
			
			bResult = false;
			logger.info("Create 7Zip Failed");
			
		}finally {
			try {
				if(out != null) {
					out.close();
				}
				if(outChannel != null) {
					outChannel.close();
				}
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return bResult;
	}
	
	/**
	  * @Method Name : un7Zip
	  * @작성일 : 2021. 9. 8.
	  * @작성자 : KYOUNGWON
	  * @변경이력 : 
	  * @Method 설명 : 압축해제
	  */
	public List<String> un7Zip(String orgPath, String tarpath) throws IOException {
		
		List<String> fileList = new ArrayList<String>();
    	
    	SevenZFile sevenZFile = new SevenZFile(new File(orgPath));
        SevenZArchiveEntry entry = sevenZFile.getNextEntry();
        
        while (entry != null) {
        	File file = new File(tarpath + File.separator + entry.getName());
            
        	String entryFileName = entry.getName();
        	
        	logger.info("EntryFileName {} ", entryFileName.toString());
        	
        	fileList.add(entryFileName);
        	
        	if (entry.isDirectory()) {
                if(!file.exists()) {
                	file.mkdirs();
                }
                
                entry = sevenZFile.getNextEntry();
                
                continue;
            }
        	
            if(!file.getParentFile().exists()) {
            	file.getParentFile().mkdirs();
            }
            
            FileOutputStream out = new FileOutputStream(file);
            byte[] content = new byte[(int) entry.getSize()];
            
            sevenZFile.read(content, 0, content.length);
            
            out.write(content);
            out.close();
            
            entry = sevenZFile.getNextEntry();
        }
        
        if(sevenZFile != null) {
        	sevenZFile.close();
        }
        
        return fileList;
	}
	
	/**
	  * @Method Name : deleteDirectoryAndFiles
	  * @작성일 : 2021. 9. 8.
	  * @작성자 : KYOUNGWON
	  * @변경이력 : 
	  * @Method 설명 : 생성한 폴더 삭제
	  */
	private boolean deleteDirectoryAndFiles(String targetFolderPath) {
        
		File targetFolder = new File(targetFolderPath);
		
		if(!targetFolder.exists()) {
            logger.error("method : deleteDirectoryAndFiles >> path not found");
            return false;
        }
        
        File[] files = targetFolder.listFiles();
        for(File file : files) {
        	
            if(file.isDirectory()) {
                
            	logger.info("Directory Name {} ", file);
            	
                deleteDirectoryAndFiles(targetFolderPath);
            }
            
            file.delete();
            /*System.out.println(file + " >>> 파일이 삭제되었습니다.");*/
            logger.info("File Delete {} ", file);
        }
        
        return targetFolder.delete();
    }
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		
		lists.add(session);
		logger.info("Session Connect Open");
	}
	
	// 클라이언트가 접속 해제 시 호출
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
	   
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