package com.visiboard.backend.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.locationtech.jts.geom.Point;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public SimpleModule jtsModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Point.class, new PointSerializer());
        module.addDeserializer(Point.class, new PointDeserializer());
        return module;
    }
}
