package com.release.rr.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // ⭐ SockJS iframe.html 대응을 위해 반드시 필요
        registry.addResourceHandler("/ws/lobby/**")
                .addResourceLocations("classpath:/META-INF/resources/");
    }
}
