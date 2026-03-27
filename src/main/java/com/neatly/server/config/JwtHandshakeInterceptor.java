package com.neatly.server.config;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates Supabase JWT from query {@code access_token} before WebSocket upgrade (rule.md / setup-backend).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

	static final String JWT_USER_SUB_ATTR = "com.neatly.jwt.sub";

	private final JwtDecoder jwtDecoder;

	@Override
	public boolean beforeHandshake(
			@NonNull ServerHttpRequest request,
			@NonNull ServerHttpResponse response,
			@NonNull WebSocketHandler wsHandler,
			@NonNull Map<String, Object> attributes) {
		if (!(request instanceof ServletServerHttpRequest servletRequest)) {
			log.debug("WebSocket handshake rejected: not a servlet request");
			return false;
		}
		String token = servletRequest.getServletRequest().getParameter("access_token");
		if (!StringUtils.hasText(token)) {
			log.debug("WebSocket handshake rejected: missing access_token query param");
			return false;
		}
		try {
			Jwt jwt = jwtDecoder.decode(token.trim());
			attributes.put(JWT_USER_SUB_ATTR, jwt.getSubject());
			return true;
		}
		catch (JwtException e) {
			log.debug("WebSocket handshake rejected: invalid JWT ({})", e.getMessage());
			return false;
		}
	}

	@Override
	public void afterHandshake(
			@NonNull ServerHttpRequest request,
			@NonNull ServerHttpResponse response,
			@NonNull WebSocketHandler wsHandler,
			Exception exception) {
		// no-op
	}
}
