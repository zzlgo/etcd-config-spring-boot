package com.zzl.etcd.config.event;

/**
 * @author zzl on 2020-03-20.
 * @description
 */
public interface Listener {

    /**
     * 配置内容变更通知
     *
     * @param configInfo
     */
    void receiveConfigInfo(String configInfo);
}