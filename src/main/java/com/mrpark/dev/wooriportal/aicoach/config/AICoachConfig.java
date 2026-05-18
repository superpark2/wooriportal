package com.mrpark.dev.wooriportal.aicoach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AICoachConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);   // 30초
        factory.setReadTimeout(120_000);     // 120초 (Ollama 응답 대기 여유)
        return new RestTemplate(factory);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/ai/aicoach/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
