package io.github.zzlgo.etcd.config;

import io.github.zzlgo.etcd.config.event.Listener;
import io.github.zzlgo.etcd.config.exception.EtcdConfigException;

/**
 * @author zzl on 2020-03-20.
 */
public interface EtcdConfigService {

    /**
     * 从配置中心获取配置
     *
     * @param dataId 配置id
     * @return 配置中心的配置
     * @throws EtcdConfigException 配置异常
     */
    String getConfig(String dataId) throws EtcdConfigException;

    /**
     * 监听dataId的配置变化
     *
     * @param dataId   配置id
     * @param listener 配置变化监听者
     */
    void addListener(String dataId, Listener listener);

    /**
     * 监听所有dataId的配置变化
     *
     * @param listener 配置变化监听者
     */
    void addAllListener(Listener listener);

}
