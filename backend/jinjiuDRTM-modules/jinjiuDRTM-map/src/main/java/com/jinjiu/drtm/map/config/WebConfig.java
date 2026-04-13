package com.jinjiu.drtm.map.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /static/road-network/** 映射到本地 resources/static/road-network/
        registry.addResourceHandler("/static/road-network/**")
                .addResourceLocations(
                    "classpath:/static/road-network/",
                    "file:src/main/resources/static/road-network/"
                );
    }
}
