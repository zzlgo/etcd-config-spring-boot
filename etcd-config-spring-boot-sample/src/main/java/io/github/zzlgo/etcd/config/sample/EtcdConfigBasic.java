package io.github.zzlgo.etcd.config.sample;

import io.github.zzlgo.etcd.config.annotation.EtcdConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 通过前缀绑定属性，是从environment里取值，不是针对某个dataId的绑定
 */
@Component
@EtcdConfigurationProperties(prefix = "basic")
public class EtcdConfigBasic {

    private String name;

    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Scheduled(cron = "0/5 * * * * *")
    public void print() {
        System.out.println("EtcdConfigBasic{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}');
    }
}