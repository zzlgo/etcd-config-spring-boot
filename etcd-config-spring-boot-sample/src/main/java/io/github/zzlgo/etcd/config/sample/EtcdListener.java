package io.github.zzlgo.etcd.config.sample;

import io.github.zzlgo.etcd.config.annotation.EtcdConfigListener;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class EtcdListener {
    /**
     * 当节点dataId变化时，触发回调
     *
     * @param properties 配置内容
     */
    @EtcdConfigListener(dataId = "/test/user.properties")
    public void onChange(Properties properties) {

        System.out.println("/test/user.properties config changed");
        for (Object t : properties.keySet()) {
            String key = String.valueOf(t);
            System.out.println(key + "," + properties.getProperty(key));
        }
    }
}