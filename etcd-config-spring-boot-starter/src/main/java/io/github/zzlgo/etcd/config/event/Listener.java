package io.github.zzlgo.etcd.config.event;

/**
 * @author zzl on 2020-03-20.
 */
public interface Listener {

    /**
     * 配置内容变更通知
     *
     * @param configInfo 配置内容
     */
    void receiveConfigInfo(String configInfo);
}