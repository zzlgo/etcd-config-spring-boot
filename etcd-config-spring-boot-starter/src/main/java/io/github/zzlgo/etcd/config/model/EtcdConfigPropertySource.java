package io.github.zzlgo.etcd.config.model;

import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

/**
 * 配置内容存储
 *
 * @author zzl on 2020-03-20.
 */
public class EtcdConfigPropertySource extends PropertiesPropertySource {

    private String dataId;
    private String type;
    private boolean autoRefreshed = true;
    private boolean first;
    private String before;
    private String after;

    public EtcdConfigPropertySource(String name, Properties source, String dataId, String type) {
        super(name, source);
        this.dataId = dataId;
        this.type = type;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAutoRefreshed() {
        return autoRefreshed;
    }

    public void setAutoRefreshed(boolean autoRefreshed) {
        this.autoRefreshed = autoRefreshed;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public void copy(EtcdConfigPropertySource oldProperties) {

        this.autoRefreshed = oldProperties.isAutoRefreshed();
        this.first = oldProperties.isFirst();
        this.before = oldProperties.getBefore();
        this.after = oldProperties.getAfter();
    }
}
