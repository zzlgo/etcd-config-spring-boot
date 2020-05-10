package com.zzl.etcd.config.component;

import com.zzl.etcd.config.autoconfigure.EtcdConfigProperties;
import com.zzl.etcd.config.util.EtcdConfigPropertiesUtil;
import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.watch.WatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * @author zzl on 2020-03-20.
 * @description
 */
public class EtcdComponent implements EnvironmentAware, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdComponent.class);

    public static final String BEAN_NAME = "etcdComponent";

    private ConfigurableEnvironment environment;
    private Client client;
    private EtcdConfigProperties imEtcdConfigProperties;

    public String getValue(String key) throws ExecutionException, InterruptedException {
        String value = "";
        KV kvClient = client.getKVClient();
        ByteSequence byteSequence = ByteSequence.from(key, StandardCharsets.UTF_8);

        GetResponse getResponse = kvClient.get(byteSequence).get();
        if (getResponse.getKvs().size() > 0) {
            KeyValue keyValue = getResponse.getKvs().get(0);
            value = Optional.ofNullable(keyValue.getValue()).map(v -> v.toString(StandardCharsets.UTF_8)).orElse("");

        }
        return value;
    }


    public void watch(String key, Consumer<String> consumer) {
        Watch watchClient = client.getWatchClient();

        ByteSequence byteSequence = ByteSequence.from(key, StandardCharsets.UTF_8);
        //watch一次即可
        watchClient.watch(byteSequence, watchResponse -> {
            for (WatchEvent event : watchResponse.getEvents()) {
                WatchEvent.EventType eventType = event.getEventType();
                if (eventType == WatchEvent.EventType.PUT) {
                    LOG.info("etcd watch put key={}", key);
                    //新增或更新
                    String value = Optional.ofNullable(event.getKeyValue().getValue()).map(v -> v.toString(StandardCharsets.UTF_8)).orElse("");
                    consumer.accept(value);
                } else if (eventType == WatchEvent.EventType.DELETE) {
                    //删除
                    LOG.warn("etcd watch ignore delete key={}", key);
                }
            }
        });

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        this.imEtcdConfigProperties = EtcdConfigPropertiesUtil.buildEtcdConfigProperties(environment);
        ClientBuilder clientBuilder = Client.builder().endpoints(imEtcdConfigProperties.getServerAddr().toArray(new String[0]));
        if (!StringUtils.isEmpty(imEtcdConfigProperties.getUsername())) {
            clientBuilder.user(ByteSequence.from(imEtcdConfigProperties.getUsername(), StandardCharsets.UTF_8));
        }
        if (!StringUtils.isEmpty(imEtcdConfigProperties.getPassword())) {
            clientBuilder.password(ByteSequence.from(imEtcdConfigProperties.getPassword(), StandardCharsets.UTF_8));
        }
        clientBuilder.loadBalancerPolicy("round_robin");
        this.client = clientBuilder.build();
        LOG.info("etcdComponent init success");
    }
}
