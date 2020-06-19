package xyz.staffjoy.common.auth;

import java.lang.annotation.*;

/**
 * 自定义鉴权注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Authorize {
    // allowed consumers
    String[] value();
}
