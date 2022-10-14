package io.github.zzlgo.etcd.config.sample;

import io.github.zzlgo.etcd.config.annotation.EtcdPropertySource;
import io.github.zzlgo.etcd.config.model.ConfigType;
import org.springframework.stereotype.Component;

/**
 * 从配置中心加载配置，支持properties和yml两种格式，自动根据后缀判断，也可以指定文件格式
 */
@Component
@EtcdPropertySource(dataId = "/test/user.properties")
@EtcdPropertySource(dataId = "/test/basic.yml", type = ConfigType.YAML)
public class EtcdConfigLoader {

}