package io.github.zzlgo.etcd.config.annotation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 参考{@link ConfigurationProperties}
 * 支持etcd配置的自动刷新
 *
 * @author zzl on 2020-03-20.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EtcdConfigurationProperties {

    /**
     * @return 配置的前缀
     */
    String prefix() default "";

    /**
     * @return 当etcd配置更新时，是否从environment刷新到bean属性
     */
    boolean autoRefreshed() default true;

    /**
     * 是否忽略不合法属性，如类型错误
     *
     * @return the flag value (default false)
     */
    boolean ignoreInvalidFields() default false;

    /**
     * 是否忽略未知属性
     *
     * @return the flag value (default true)
     */
    boolean ignoreUnknownFields() default true;
}