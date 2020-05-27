package com.ypw.starter.shardingsphere.config;
 
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sharding.ds1")
public class SecondDsProp {
    private String jdbcUrl;
    private String username;
    private String password;
    private String type;
}