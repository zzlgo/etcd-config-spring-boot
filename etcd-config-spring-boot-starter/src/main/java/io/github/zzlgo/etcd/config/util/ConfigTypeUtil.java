package io.github.zzlgo.etcd.config.util;

import io.github.zzlgo.etcd.config.model.ConfigType;

/**
 * @author zzl on 2020-04-11.
 */
public class ConfigTypeUtil {

    private static final String PROPERTIES_SUFFIX = ".properties";
    private static final String YAML_SUFFIX = ".yml";
    private static final String YAML_SUFFIX2 = ".yaml";

    /**
     * 获取配置文件类型
     *
     * @param configType 配置类型枚举
     * @param dataId     配置id
     * @return 配置类型字符串
     */
    public static String getTypeWithDataId(ConfigType configType, String dataId) {
        String type = configType.getType();
        if (dataId.endsWith(PROPERTIES_SUFFIX)) {
            type = ConfigType.PROPERTIES.getType();
        } else if (dataId.endsWith(YAML_SUFFIX) || dataId.endsWith(YAML_SUFFIX2)) {
            type = ConfigType.YAML.getType();
        }
        return type;
    }
}
