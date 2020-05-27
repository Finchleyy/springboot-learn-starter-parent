package com.ypw.springbootlearnstarter.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yupengwu
 */
@ConfigurationProperties(prefix = "ypw")
@Data
public class YpwProperties {
    private Environment environment;
    private Boolean enable;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Environment {
        Boolean enable;
        ServerConfig dev;
        ServerConfig prod;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ServerConfig {
        String url;
        String name;
    }

}
