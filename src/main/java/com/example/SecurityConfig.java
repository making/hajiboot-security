package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http.authorizeHttpRequests(requests -> requests //
						.mvcMatchers("/webjars/**").permitAll() //
						.mvcMatchers("/css/**").permitAll() //
						.anyRequest().authenticated()) //
				.formLogin(login -> login //
						.loginProcessingUrl("/login") //
						.loginPage("/loginForm").permitAll() //
						.failureUrl("/loginForm?error") //
						.defaultSuccessUrl("/customers", true)) //
				.logout(logout -> logout.logoutSuccessUrl("/loginForm"))
				.build();
	}
}