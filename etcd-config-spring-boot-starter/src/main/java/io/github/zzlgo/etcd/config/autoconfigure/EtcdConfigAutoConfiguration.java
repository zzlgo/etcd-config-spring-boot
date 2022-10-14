package io.github.zzlgo.etcd.config.autoconfigure;

import io.github.zzlgo.etcd.config.annotation.EnableEtcdConfig;
import io.github.zzlgo.etcd.config.model.EtcdConfigConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zzl on 2020-03-16.
 */
@Configuration
@EnableConfigurationProperties(value = EtcdConfigProperties.class)
@ConditionalOnProperty(name = EtcdConfigConstants.CONFIG_PROPERTIES_ENABLED, havingValue = "true")
@EnableEtcdConfig
public class EtcdConfigAutoConfiguration {

}
