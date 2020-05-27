package com.ypw.starter.shardingsphere.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 这种方式需要主动指定 enable 注解才能生效
 *
 * @author yupengwu
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DataSourceConfig.class)
public @interface EnableShardingSphere {
}
