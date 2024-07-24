package com.smart.config;


import java.text.Normalizer.Form;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class MyConfig{

	@Bean
	public UserDetailsService getUserDetailsService() {
		return new UserDetailsServiceImpl();
	}
	
	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(this.getUserDetailsService());
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(registry->{
        	registry.requestMatchers("/admin/**").hasRole("ADMIN");
        	registry.requestMatchers("/user/**").hasRole("USER");
        	registry.requestMatchers("/**").permitAll();
        	registry.anyRequest().authenticated();
        })
        .formLogin(form -> form.
        		loginPage("/signin")
        		.defaultSuccessUrl("/user/index")
        		.permitAll()
        		)
//        		.formLogin(Customizer.withDefaults())
        .logout(logout -> logout
        		.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
        		.permitAll()
        		)
        .build();
    }

}
