package io.github.zzlgo.etcd.config.annotation;

import io.github.zzlgo.etcd.config.component.EtcdConfigBeanDefinitionRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zzl on 2020-03-24.
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EtcdConfigBeanDefinitionRegistrar.class)
public @interface EnableEtcdConfig {

}
