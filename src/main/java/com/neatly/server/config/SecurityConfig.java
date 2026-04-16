package com.neatly.server.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.neatly.server.security.SupabaseRoleResolver;
import com.neatly.server.service.UserProvisioningService;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

	@Bean
	UserProvisioningFilter userProvisioningFilter(UserProvisioningService userProvisioningService) {
		return new UserProvisioningFilter(userProvisioningService);
	}

	@Bean
	Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter(
			SupabaseRoleResolver supabaseRoleResolver) {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			String role = supabaseRoleResolver.resolveRole(jwt);
			GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT));
			return List.of(authority);
		});
		return converter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			UserProvisioningFilter userProvisioningFilter,
			Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter)
			throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/actuator/info", "/actuator/info/**").permitAll()
						.requestMatchers("/api/v1/webhooks/stripe").permitAll()
						.requestMatchers("/api/v1/extra-services").permitAll()
						.requestMatchers("/api/v1/promotions/validate").permitAll()
						.requestMatchers("/api/v1/public/**").permitAll()
						.requestMatchers("/ws", "/ws/**").permitAll()
						.requestMatchers("/api/v1/chat/presets", "/api/v1/chat/presets/**", "/api/v1/chat/search",
								"/api/v1/chat/defaults")
								.permitAll()
						.requestMatchers("/api/v1/chat/rooms/pending").hasRole("ADMIN")
						.requestMatchers("/api/v1/chat/rooms/my-active").hasRole("ADMIN")
						.requestMatchers("/api/v1/chat/rooms/*/accept").hasRole("ADMIN")
						.requestMatchers("/api/v1/chat/rooms/*/leave").hasRole("ADMIN")
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.addFilterAfter(userProvisioningFilter, BearerTokenAuthenticationFilter.class);
		log.debug("SecurityFilterChain configured (stateless JWT, public health + Stripe webhook + /ws + chat presets)");
		return http.build();
	}
}
