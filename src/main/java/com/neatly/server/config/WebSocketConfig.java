package com.neatly.server.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * STOMP over WebSocket configuration for live chat.
 *
 * Clients connect to /ws/chat with ?access_token=... query param or
 * via STOMP CONNECT header "Authorization: Bearer ...".
 *
 * Topics:
 *   /topic/admin/chat-requests  — broadcast new chat requests to all admins
 *   /topic/admin/chat-taken     — broadcast when a room is accepted
 *   /user/{userId}/queue/chat   — per-user chat messages
 *   /user/{userId}/queue/chat-events — per-user events (AGENT_JOINED, etc.)
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final String allowedOriginPatterns;
	private final JwtDecoder jwtDecoder;

	public WebSocketConfig(
			@Value("${app.cors.allowed-origins}") String allowedOrigins,
			JwtDecoder jwtDecoder) {
		this.allowedOriginPatterns = allowedOrigins;
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic", "/queue");
		config.setApplicationDestinationPrefixes("/app");
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		String[] patterns = Arrays.stream(allowedOriginPatterns.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.toArray(String[]::new);
		if (patterns.length == 0) {
			patterns = new String[] { "http://localhost:5173" };
		}
		registry.addEndpoint("/ws/chat")
				.setAllowedOriginPatterns(patterns)
				.withSockJS();

		// Also register without SockJS for native WebSocket clients
		registry.addEndpoint("/ws/chat")
				.setAllowedOriginPatterns(patterns);
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
						StompHeaderAccessor.class);
				if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
					String token = extractToken(accessor);
					if (token == null) {
						log.warn("WebSocket STOMP CONNECT rejected: missing bearer token");
						return null;
					}
					try {
						Jwt jwt = jwtDecoder.decode(token);
						String userId = jwt.getSubject();
						UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
								userId, null, java.util.List.of());
						accessor.setUser(auth);
						log.debug("WebSocket STOMP authenticated userId={}", userId);
					} catch (Exception e) {
						log.warn("WebSocket STOMP CONNECT rejected: {}", e.getMessage());
						return null;
					}
				}
				return message;
			}
		});
	}

	private String extractToken(StompHeaderAccessor accessor) {
		// Try Authorization header first
		String authHeader = accessor.getFirstNativeHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}
		// Try access_token header (some clients send it this way)
		String tokenHeader = accessor.getFirstNativeHeader("access_token");
		if (tokenHeader != null) {
			return tokenHeader;
		}
		return null;
	}
}
