package com.zzl.etcd.config.autoconfigure;

import com.zzl.etcd.config.model.EtcdConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author zzl on 2020-03-16.
 * @description
 */
@ConfigurationProperties(prefix = EtcdConfigConstants.CONFIG_PROPERTIES_PREFIX)
public class EtcdConfigProperties {

    private boolean enabled;
    private List<String> serverAddr;
    private String username;
    private String password;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(List<String> serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    @Override
    public String toString() {
        return "ImEtcdConfigProperties{" +
                "enabled=" + enabled +
                ", serverAddr=" + serverAddr +
                ", username='" + username + '\'' +
                ", password='" + "******" + '\'' +
                '}';
    }
}
