package io.github.zzlgo.etcd.config.annotation;

import io.github.zzlgo.etcd.config.model.ConfigType;
import org.springframework.core.env.PropertySource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参考{@link PropertySource}
 * 加载etcd中的配置
 *
 * @author zzl on 2020-03-20.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(EtcdPropertySources.class)
public @interface EtcdPropertySource {

    /**
     * 加载etcd中的dataId的配置到environment
     *
     * @return dataId
     */
    String dataId();

    /**
     * @return 加载到environment中的配置文件名称，默认为dataId
     */
    String name() default "";

    /**
     * 当etcd配置更新时，是否自动刷新配置到environment
     *
     * @return 是否自动刷新
     */
    boolean autoRefreshed() default true;

    /**
     * @return 配置的内容类型，如果dataId以.properties结尾或.yml结尾，则忽略这个type值
     */
    ConfigType type() default ConfigType.PROPERTIES;

    /**
     * 配置优先级
     * 如果{@link #first()}为true，则优先级最高，并且忽略{@link #before()} 和 {@link #after()}
     *
     * @return 最高优先级
     */
    boolean first() default false;

    /**
     * 配置优先级
     * 比{@link #before()}优先级高
     *
     * @return the name of {@link PropertySource}
     */
    String before() default "";

    /**
     * 配置优先级
     * 比{@link #after()}优先级低
     *
     * @return the name of {@link PropertySource}
     */
    String after() default "";
}
