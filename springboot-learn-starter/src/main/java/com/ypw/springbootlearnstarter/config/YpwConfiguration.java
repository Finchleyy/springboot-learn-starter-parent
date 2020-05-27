package com.ypw.springbootlearnstarter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yupengwu
 */
@EnableConfigurationProperties(YpwProperties.class)
@Configuration
@ConditionalOnProperty(prefix = "ypw", name = "enable", havingValue = "true")
public class YpwConfiguration {
    @Bean("ypwDev")
    @ConditionalOnProperty(prefix = "ypw.environment.dev", name = "enable", havingValue = "true")
    public YpwBean ypwBeanDev(YpwProperties ypwProperties) {
        YpwBean ypwBean = new YpwBean();
        ypwBean.setName(ypwProperties.getEnvironment().getDev().getName());
        return ypwBean;
    }
}
