package com.example;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.stereotype.Component;

@Component
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests() //
            .mvcMatchers("/webjars/**", "/css/**").permitAll() //
            .anyRequest().authenticated() //
            .and() //
            .formLogin() //
            .loginProcessingUrl("/login") //
            .loginPage("/loginForm").permitAll() //
            .failureUrl("/loginForm?error") //
            .defaultSuccessUrl("/customers", true) //
            .and() //
            .logout().logoutSuccessUrl("/loginForm");
    }
}