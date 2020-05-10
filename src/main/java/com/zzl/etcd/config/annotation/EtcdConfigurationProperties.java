package com.zzl.etcd.config.annotation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.annotation.*;


/**
 * 参考{@link ConfigurationProperties}
 * 支持etcd配置的自动刷新
 *
 * @author zzl on 2020-03-20.
 * @description
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EtcdConfigurationProperties {

    /**
     * config prefix name
     *
     * @return default value is <code>""</code>
     */
    String prefix() default "";

    /**
     * 当etcd配置更新时，是否从environment刷新到bean属性
     *
     * @return
     */
    boolean autoRefreshed() default true;

    /**
     * Flag to indicate that when binding to this object invalid fields should be ignored.
     * Invalid means invalid according to the binder that is used, and usually this means
     * fields of the wrong type (or that cannot be coerced into the correct type).
     *
     * @return the flag value (default false)
     */
    boolean ignoreInvalidFields() default false;

    /**
     * Flag to indicate that when binding to this object unknown fields should be ignored.
     * An unknown field could be a sign of a mistake in the Properties.
     *
     * @return the flag value (default true)
     */
    boolean ignoreUnknownFields() default true;
}