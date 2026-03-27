package com.neatly.server.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocket
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

	private final String allowedOriginPatterns;
	private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

	public WebSocketConfig(
			@Value("${app.cors.allowed-origins}") String allowedOrigins,
			JwtHandshakeInterceptor jwtHandshakeInterceptor) {
		this.allowedOriginPatterns = allowedOrigins;
		this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		String[] patterns = Arrays.stream(allowedOriginPatterns.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.toArray(String[]::new);
		if (patterns.length == 0) {
			patterns = new String[] { "http://localhost:5173" };
		}
		registry.addHandler(neatlyEchoHandler(), "/ws")
				.addInterceptors(jwtHandshakeInterceptor)
				.setAllowedOriginPatterns(patterns);
		log.debug("WebSocket registered at /ws (JWT handshake via access_token query param)");
	}

	private WebSocketHandler neatlyEchoHandler() {
		return new TextWebSocketHandler() {
			@Override
			protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
				log.trace("WebSocket echo sessionId={} payloadLength={}", session.getId(),
						message.getPayload() != null ? message.getPayload().length() : 0);
				session.sendMessage(new TextMessage("echo: " + message.getPayload()));
			}
		};
	}
}
