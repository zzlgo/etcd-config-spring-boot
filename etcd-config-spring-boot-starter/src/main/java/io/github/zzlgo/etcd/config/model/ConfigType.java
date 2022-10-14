package io.github.zzlgo.etcd.config.model;

/**
 * @author zzl on 2020-03-20.
 */
public enum ConfigType {

    PROPERTIES("properties"),

    YAML("yaml");

    String type;

    ConfigType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}