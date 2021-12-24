package io.github.gcdd1993.casdemo.app1.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = { java.lang.annotation.ElementType.TYPE })
@Documented
@Import({DefaultSecurityConfiguration.class, DefaultPatternController.class})
public @interface EnableDefaultSecurityConfiguration {
    /**
     * 仅限调试用，使全部接口跳过认证
     */
    boolean disableAuth() default false;

    /**
     * 仅限调试用，对CAS服务的调用禁用SSL证书检查
     */
    boolean disableSslCheck() default false;
}
