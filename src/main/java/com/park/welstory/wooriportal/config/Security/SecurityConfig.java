package com.park.welstory.wooriportal.config.security;

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
                        .requestMatchers(
                                "/style/**", "/js/**", "/font/**", "/bg/**", "/file/**",
                                "/loading/**", "/mol/**", "/weather/**", "/filestorage/**", "/login",
                                "/signup", "/pcinfo/view/**", "/facility/pcinfo/pclist", "/facility/pcinfo/add",
                                "/facility/pcinfo/verify-password", "/facility/pcinfo/delete",
                                "/pcinfo/require/**", "/location/**", "/log/**", "/db/**",
                                "/api/sse/**" // SSE 연결 허용

                        ).permitAll()

                        .requestMatchers("/bg/**", "/file/**", "/font/**", "/js/**", "/loading/**",
                                "/mol/**", "/style/**", "/weather/**").permitAll()
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