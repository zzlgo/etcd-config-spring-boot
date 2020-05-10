package com.zzl.etcd.config.exception;

/**
 * @author zzl on 2020-03-20.
 * @description
 */
public class EtcdConfigException extends RuntimeException {

    public EtcdConfigException(String message) {
        super(message);
    }

    public EtcdConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
