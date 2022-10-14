package io.github.zzlgo.etcd.config.annotation;

import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参考{@link Value}
 * 支持etcd配置的自动刷新
 *
 * @author zzl on 2020-03-20.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EtcdValue {

    /**
     * @return 配置的值
     */
    String value();

    /**
     * @return 当etcd配置更新时，是否从environment刷新到bean属性
     */
    boolean autoRefreshed() default true;

}