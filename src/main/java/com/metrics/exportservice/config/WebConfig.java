package com.metrics.exportservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                    "http://localhost:*",
                    "https://user:*@application-development-analyzer-tunnel-gmxqxmbk.devinapps.com",
                    "https://application-development-analyzer-tunnel-gmxqxmbk.devinapps.com",
                    "https://*application-development-analyzer-tunnel-gmxqxmbk.devinapps.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Content-Disposition")
                .maxAge(3600);
    }
}
