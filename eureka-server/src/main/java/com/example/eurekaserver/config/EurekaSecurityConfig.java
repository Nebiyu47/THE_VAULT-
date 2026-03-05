package com.example.eurekaserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class EurekaSecurityConfig {

    @Bean
    public SecurityFilterChain serverFilterChain(HttpSecurity http) throws Exception {
        http
                // This is the "Magic Fix": Eureka clients can't send CSRF tokens
                .csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(withDefaults());

        return http.build();
    }
}