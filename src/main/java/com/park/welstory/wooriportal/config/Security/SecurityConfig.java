package com.park.welstory.wooriportal.config.Security;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. 정적 리소스 및 공통 UI 요소 (모두 허용)
                        .requestMatchers(
                                "/style/**", "/js/**", "/font/**", "/bg/**", "/file/**",
                                "/loading/**", "/mol/**", "/weather/**", "/tutil/**"
                        ).permitAll()

                        // 2. 인증/인가 없이 접근 가능한 공통 페이지 및 파일 저장소
                        .requestMatchers("/login", "/signup", "/filestorage/**").permitAll()

                        // 3. 서비스 관련 API 및 기능 경로 (모두 허용)
                        .requestMatchers(
                                "/pcinfo/**",             // view, require 등 하위 경로 포함
                                "/facility/pcinfo/**",    // pclist, add, delete 등 하위 경로 포함
                                "/location/**", "/log/**", "/db/**", "/saramin/**",
                                "/api/sse/**", "/ai/**", "/excel/**"
                        ).permitAll()

                        // 4. 그 외 모든 요청은 권한(user 또는 admin)이 있어야 접근 가능
                        .anyRequest().hasAnyAuthority("user", "admin")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecret")
                        .tokenValiditySeconds(604800) // 7일
                        .userDetailsService(customUserDetailsService)
                        .rememberMeParameter("remember-me")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID", "remember-me") // 쿠키 삭제
                        .permitAll()
                )
                .userDetailsService(customUserDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}