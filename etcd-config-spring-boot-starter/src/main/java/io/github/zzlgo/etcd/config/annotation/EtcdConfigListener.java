package io.github.zzlgo.etcd.config.annotation;

import io.github.zzlgo.etcd.config.model.ConfigType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当etcd中的dataId的配置发生变化时，触发回调
 *
 * @author zzl on 2020-03-20.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface EtcdConfigListener {

    /**
     * 配置集
     *
     * @return dataId
     */
    String dataId();

    /**
     * @return 配置的内容类型，如果dataId以.properties结尾或.yml结尾，则忽略这个type值
     */
    ConfigType type() default ConfigType.PROPERTIES;

    /**
     * @return 最大执行时间，单位毫秒
     */
    long timeout() default 5000L;

}