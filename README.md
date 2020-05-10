# 配置
### 1.maven配置
```xml
<dependency>  
    <groupId>com.zzl</groupId>
    <artifactId>etcd-config-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2.application.yml配置
```yaml
etcd:
  config:
    # 开启
    enabled: true
    server-addr: 
    username: 
    password: 
```
          
# 使用
### 1.@EtcdPropertySource
```text
作用：从etcd加载配置到environment。
dataId代表etcd中的节点全路径，是配置集的意思，不是配置的key。
配置内容只支持properties和yml两种格式，默认是properties格式。设置内容格式，dataId以.properties结尾或.yml结尾，或者在@EtcdPropertySource注解中手动设置格式。
可以通过@EtcdPropertySource设置配置的优先级，默认优先级最低。
```
```java
@Component
@EtcdPropertySource(dataId = "/test/user.properties")
@EtcdPropertySource(dataId = "/test/menu.yml")
public class EtcdConfigLoader {
 
}
```

### 2.@EtcdValue
```text
作用：自动注入属性，对比spring的@Value，支持属性的自动刷新
```
```java
@Component
public class EtcdConfig {
 
    /**
     * 自动注入，自动刷新
     */
    @EtcdValue(value = "${username}")
    private String username;
 
    public String getUsername() {
        return username;
    }
 
}
```

### 3.@EtcdConfigurationProperties
```text
作用：属性绑定，对比spring的@ConfigurationProperties，支持属性的自动刷新
```
```java
@Component
//通过前缀绑定属性，是从environment里取值，不是针对某个dataId的绑定
@EtcdConfigurationProperties(prefix = "user")
public class EtcdUserProperties {
 
    private List<String> allowedUids = new ArrayList<>();
 
    public List<String> getAllowedUids() {
        return allowedUids;
    }
 
}
```

### 4.@EtcdConfigListener
```text
在调用用户自定义的方法时，保证属性已经更新到environment，已经更新到对象属性上，最后才执行这个通知方法。
自定义方法的参数类型为Properties或String。若是Properties类型，不需要自己解析配置文件的内容了。
需要注意的是，这个变化是基于dataId产生的，如果dataId中有多个key，是不知道哪个key发生的变化。
```
```java
@Component
public class EtcdListener {
 
    private static final Logger LOG = LoggerFactory.getLogger(EtcdListener.class);
 
    /**
     * 当节点dataId变化时，触发回调
     *
     * @param properties
     */
    @EtcdConfigListener(dataId = "/test/user.properties")
    public void onChange(Properties properties) {
        LOG.info("onChange");
        for (Object t : properties.keySet()) {
            String key = String.valueOf(t);
            LOG.info(key + "," + properties.getProperty(key));
        }
    }
}
```
### 5.对比spring注解

| spring | etcd config starter | 功能 |
| :----- | :---- | :---- |
| @Value | @EtcdValue | 属性自动注入 |
| @ConfigurationProperties | @EtcdConfigurationProperties | 属性绑定 |
| @PropertySource | 	@EtcdPropertySource | 加载配置 |
|  | 	@@EtcdConfigListener | 自定义通知方法 |

# 原理
<img src="doc/etcd-starter.png" width="80%" />

```text
主要流程分为4步
（1）@EtcdPropertySource注解解析，从etcd读取配置加载到environment，可支持多个dataId，每个dataId对应一个etcdPropertySource。
优先级默认最低，多个dataId之间优先级按加载顺序，尽量避免优先级不同的重复配置项。
（2）@EtcdValue和@EtcdConfigurationProperties的属性自动注入
（3）注册etcd的watch，监听配置变更，每个注解注册的listener可能类型不同，底层共享etcd的watch
（4）etcd配置变更，触发回调。所有注册的listener按优先级分为三类，按顺序执行。每个类型的listener保证顺序。

第一类：@EtcdPropertySource注解的listener，负责刷新environment
第二类：@EtcdValue和@EtcdConfigurationProperties注解的listener，负责属性的重新注入
第三类：@EtcdConfigListener注解的listener，负责执行用户自定义方法

```