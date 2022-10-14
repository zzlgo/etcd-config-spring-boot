package io.github.zzlgo.etcd.config.sample;

import io.github.zzlgo.etcd.config.annotation.EtcdValue;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EtcdConfig {

    /**
     * 自动注入，配置中心更改后会自动刷新
     */
    @EtcdValue(value = "${code}")
    private String code;

    /**
     * 自动注入，配置中心更改后不会自动刷新
     */
    @EtcdValue(value = "${city}", autoRefreshed = false)
    private String city;

    public String getCode() {
        return code;
    }

    public String getCity() {
        return city;
    }

    @Scheduled(cron = "0/5 * * * * *")
    public void print() {
        System.out.println("EtcdConfig{" +
                "code='" + code + '\'' +
                ", city='" + city + '\'' +
                '}');
    }
}