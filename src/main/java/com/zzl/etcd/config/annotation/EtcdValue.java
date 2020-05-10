package com.zzl.etcd.config.annotation;

import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.*;

/**
 * 参考{@link Value}
 * 支持etcd配置的自动刷新
 *
 * @author zzl on 2020-03-20.
 * @description
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EtcdValue {

    /**
     * The actual value expression: for example {@code #{systemProperties.myProp}}.
     */
    String value();

    /**
     * 当etcd配置更新时，是否从environment刷新到bean属性
     *
     * @return
     */
    boolean autoRefreshed() default true;

}