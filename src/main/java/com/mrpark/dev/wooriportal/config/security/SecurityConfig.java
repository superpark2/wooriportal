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
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
                // - plain CsrfTokenRequestAttributeHandler: 쿠키 원본값과 헤더값을 그대로 비교(JS 연동용).
                //   기본 Xor 핸들러는 응답 토큰을 인코딩해 쿠키값과 불일치 → 403 유발.
                // - 머신/외부 연동 엔드포인트(HRD 세션·템플릿 하베스터)는 토큰을 보낼 수 없으므로 예외 처리.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // AI/엑셀 스트리밍 + 채용정보(로그인 불필요) + 읽기 전용 ID 중복확인은 제외
                        .ignoringRequestMatchers("/ai/**", "/excel/**", "/jobinfo/**", "/check-id")
                )
                // Spring Security 6의 지연 토큰 로딩 대응: 매 요청마다 토큰을 강제로 로드해
                // XSRF-TOKEN 쿠키가 항상 응답에 실리도록 함(없으면 JS가 토큰을 못 읽어 403).
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // 1. 정적 리소스 및 공통 UI 요소 (모두 허용)
                        .requestMatchers(
                                "/style/**", "/js/**", "/font/**", "/bg/**", "/file/**",
                                "/loading/**", "/weather/**", "/tutil/**"
                        ).permitAll()

                        // 2. 인증/인가 없이 접근 가능한 공통 페이지
                        //    (filestorage·db는 민감 데이터/관리 기능이므로 permitAll에서 제거)
                        .requestMatchers("/login", "/signup", "/check-id", "/aicoach/**", "/jobinfo/**").permitAll()

                        // 3. DB 관리 화면 및 API는 관리자 전용
                        .requestMatchers("/db/**").hasAuthority("admin")

                        // 4. 그 외 서비스 API는 로그인(user 또는 admin) 필요
                        .requestMatchers(
                                "/pcinfo/**",             // view, require 등 하위 경로 포함
                                "/facility/pcinfo/**",    // pclist, add, delete 등 하위 경로 포함
                                "/location/**", "/log/**",
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

    /**
     * CsrfToken을 매 요청에서 강제로 렌더링(getToken 호출)하여
     * CookieCsrfTokenRepository가 XSRF-TOKEN 쿠키를 응답에 반드시 쓰도록 한다.
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken(); // 토큰 로드 → 쿠키 기록 트리거
            }
            filterChain.doFilter(request, response);
        }
    }
}