package io.github.zzlgo.etcd.config.exception;

/**
 * @author zzl on 2020-03-20.
 */
public class EtcdConfigException extends RuntimeException {

    public EtcdConfigException(String message) {
        super(message);
    }

    public EtcdConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
