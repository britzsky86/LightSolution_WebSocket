package com.posmania.kr.Config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.posmania.kr.Handler.WebSocketFileHandlerV3;
import com.posmania.kr.Handler.WebSocketHandler;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
	
    private final WebSocketHandler webSocketHandler;
    private final WebSocketFileHandlerV3 webSocketFileHandlerV3;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/sync").setAllowedOrigins("*");
        registry.addHandler(webSocketFileHandlerV3, "/ws/file").setAllowedOrigins("*");
    }
    
    @Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(1000000);		// 테스트 메시지의 최대 크기 설정.
		container.setMaxBinaryMessageBufferSize(50000000);	// 50메가바이트
		/*container.setMaxSessionIdleTimeout(TimeUnit.MINUTES.convert(1, TimeUnit.MILLISECONDS));*/	// 웹 소켓 세션 유지 시간 설정(1분).
		container.setMaxSessionIdleTimeout(104000L);													// 2분	
		container.setAsyncSendTimeout(TimeUnit.SECONDS.convert(10, TimeUnit.MILLISECONDS));			// 응답을 보내기 위해 시도하는 시간 설정(5초).
		return container;
	}
}