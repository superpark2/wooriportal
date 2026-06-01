package com.mrpark.dev.wooriportal.config.security;

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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final com.mrpark.dev.wooriportal.config.security.CustomUserDetailsService customUserDetailsService;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF: 쿠키 기반 토큰(XSRF-TOKEN) 발급 → 프론트엔드 fetch가 X-XSRF-TOKEN 헤더로 회신.
                // 머신/외부 연동 엔드포인트(오토잇 에이전트, QR/비컨 출결)는 토큰을 보낼 수 없으므로 예외 처리.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/coolapi/**")
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. 정적 리소스 및 공통 UI 요소 (모두 허용)
                        .requestMatchers(
                                "/style/**", "/js/**", "/font/**", "/bg/**", "/file/**",
                                "/loading/**", "/weather/**", "/tutil/**"
                        ).permitAll()

                        // 2. 인증/인가 없이 접근 가능한 공통 페이지
                        //    (filestorage·db는 민감 데이터/관리 기능이므로 permitAll에서 제거)
                        .requestMatchers("/login", "/signup", "/aicoach/**", "/coolapi/**").permitAll()

                        // 3. DB 관리 화면 및 API는 관리자 전용
                        .requestMatchers("/db/**").hasAuthority("admin")

                        // 4. 그 외 서비스 API는 로그인(user 또는 admin) 필요
                        .requestMatchers(
                                "/pcinfo/**",             // view, require 등 하위 경로 포함
                                "/facility/pcinfo/**",    // pclist, add, delete 등 하위 경로 포함
                                "/location/**", "/log/**", "/saramin/**",
                                "/filestorage/**",
                                "/api/sse/**", "/ai/**", "/excel/**"
                        ).hasAnyAuthority("user", "admin")

                        // 5. 그 외 모든 요청은 권한(user 또는 admin)이 있어야 접근 가능
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