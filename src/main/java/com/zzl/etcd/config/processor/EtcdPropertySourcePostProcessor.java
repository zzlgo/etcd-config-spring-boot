package com.zzl.etcd.config.processor;

import com.zzl.etcd.config.EtcdConfigService;
import com.zzl.etcd.config.annotation.EtcdPropertySource;
import com.zzl.etcd.config.annotation.EtcdPropertySources;
import com.zzl.etcd.config.component.EtcdConfigServiceImpl;
import com.zzl.etcd.config.event.UpdateCacheListener;
import com.zzl.etcd.config.exception.EtcdConfigException;
import com.zzl.etcd.config.model.ConfigType;
import com.zzl.etcd.config.model.EtcdConfigPropertySource;
import com.zzl.etcd.config.util.ConfigParseUtil;
import com.zzl.etcd.config.util.ConfigTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author zzl on 2020-03-20.
 * @description 加载配置内容到environment
 */
public class EtcdPropertySourcePostProcessor implements BeanFactoryPostProcessor, EnvironmentAware, ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdPropertySourcePostProcessor.class);

    public static final String BEAN_NAME = "etcdPropertySourcePostProcessor";


    private ConfigurableEnvironment environment;
    private final Set<String> processedBeanNames = new LinkedHashSet<String>();
    private EtcdConfigService etcdConfigService;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String[] beanNames = beanFactory.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            processPropertySource(beanName, beanFactory);
        }
    }

    private void processPropertySource(String beanName, ConfigurableListableBeanFactory beanFactory) {
        if (processedBeanNames.contains(beanName)) {
            return;
        }

        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        if (!AnnotatedBeanDefinition.class.isAssignableFrom(beanDefinition.getClass())) {
            return;
        }

        List<EtcdConfigPropertySource> etcdPropertySourceList = buildEtcdPropertySources(beanName, (AnnotatedBeanDefinition) beanDefinition);

        for (EtcdConfigPropertySource etcdConfigPropertySource : etcdPropertySourceList) {
            addEtcdPropertySource(etcdConfigPropertySource);
            addListenerIfAutoRefreshed(etcdConfigPropertySource);
        }

        processedBeanNames.add(beanName);
    }

    private void addListenerIfAutoRefreshed(EtcdConfigPropertySource oldProperties) {
        if (!oldProperties.isAutoRefreshed()) {
            return;
        }

        etcdConfigService.addListener(oldProperties.getDataId(), new UpdateCacheListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                MutablePropertySources propertySources = environment.getPropertySources();

                Properties properties = ConfigParseUtil.toProperties(configInfo, oldProperties.getType());

                EtcdConfigPropertySource newProperties = new EtcdConfigPropertySource(oldProperties.getName(), properties, oldProperties.getDataId(), oldProperties.getType());
                newProperties.copy(oldProperties);
                //替换配置内容
                propertySources.replace(newProperties.getName(), newProperties);
                LOG.info("updateCacheListener replace etcd config,dataId={}", newProperties.getDataId());
            }
        });
        LOG.info("add updateCacheListener dataId={}", oldProperties.getDataId());
    }

    private void addEtcdPropertySource(EtcdConfigPropertySource etcdConfigPropertySource) {
        MutablePropertySources propertySources = environment.getPropertySources();

        boolean first = etcdConfigPropertySource.isFirst();
        String before = etcdConfigPropertySource.getBefore();
        String after = etcdConfigPropertySource.getAfter();

        boolean hasBefore = !StringUtils.isEmpty(before);
        boolean hasAfter = !StringUtils.isEmpty(after);

        boolean isRelative = hasBefore || hasAfter;

        if (first) {
            // 优先级最高
            propertySources.addFirst(etcdConfigPropertySource);
        } else if (isRelative) {
            //设置优先级
            if (hasBefore) {
                propertySources.addBefore(before, etcdConfigPropertySource);
            }
            if (hasAfter) {
                propertySources.addAfter(after, etcdConfigPropertySource);
            }
        } else {
            // 默认优先级最低
            propertySources.addLast(etcdConfigPropertySource);
        }
        LOG.info("addEtcdPropertySource dataId={}", etcdConfigPropertySource.getDataId());
    }

    private List<EtcdConfigPropertySource> buildEtcdPropertySources(String beanName, AnnotatedBeanDefinition annotatedBeanDefinition) {

        List<Map<String, Object>> annotationAttributesList = getAnnotationAttributesList(annotatedBeanDefinition);

        List<EtcdConfigPropertySource> list = new ArrayList<>();
        for (Map<String, Object> annotationAttributes : annotationAttributesList) {
            //解析注解内容，根据dataId获取配置信息
            String dataId = (String) annotationAttributes.get("dataId");
            String name = (String) annotationAttributes.get("name");
            ConfigType configType = ((ConfigType) annotationAttributes.get("type"));

            if (StringUtils.isEmpty(dataId)) {
                throw new IllegalArgumentException("dataId is null");
            }

            String type = ConfigTypeUtil.getTypeWithDataId(configType, dataId);

            if (StringUtils.isEmpty(name)) {
                name = dataId;
            }
            //从配置中心加载配置内容
            String config = etcdConfigService.getConfig(dataId);
            if (StringUtils.isEmpty(config)) {
                throw new EtcdConfigException("config is null,dataId=" + dataId);
            }

            Properties properties = ConfigParseUtil.toProperties(config, type);

            EtcdConfigPropertySource etcdConfigPropertySource = new EtcdConfigPropertySource(name, properties, dataId, type);

            setOther(etcdConfigPropertySource, annotationAttributes);
            list.add(etcdConfigPropertySource);
        }

        return list;
    }


    /**
     * 查找EtcdPropertySource注解的bean
     *
     * @param annotatedBeanDefinition
     * @return
     */
    private List<Map<String, Object>> getAnnotationAttributesList(AnnotatedBeanDefinition annotatedBeanDefinition) {
        List<Map<String, Object>> annotationAttributesList = new LinkedList<>();

        AnnotationMetadata metadata = annotatedBeanDefinition.getMetadata();
        Set<String> annotationTypes = metadata.getAnnotationTypes();
        for (String annotationType : annotationTypes) {

            if (EtcdPropertySources.class.getName().equals(annotationType)) {
                Map<String, Object> annotationAttributes = metadata
                        .getAnnotationAttributes(annotationType);
                if (annotationAttributes != null) {
                    annotationAttributesList.addAll(Arrays.asList(
                            (Map<String, Object>[]) annotationAttributes.get("value")));
                }
            } else if (EtcdPropertySource.class.getName().equals(annotationType)) {
                Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(annotationType);
                annotationAttributesList.add(annotationAttributes);
            }
        }
        return annotationAttributesList;
    }

    private void setOther(EtcdConfigPropertySource etcdConfigPropertySource, Map<String, Object> annotationAttributes) {
        boolean autoRefreshed = Boolean.TRUE.equals(annotationAttributes.get("autoRefreshed"));
        boolean first = Boolean.TRUE.equals(annotationAttributes.get("first"));
        String before = (String) annotationAttributes.get("before");
        String after = (String) annotationAttributes.get("after");

        etcdConfigPropertySource.setAutoRefreshed(autoRefreshed);
        etcdConfigPropertySource.setFirst(first);
        etcdConfigPropertySource.setBefore(before);
        etcdConfigPropertySource.setAfter(after);

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.etcdConfigService = applicationContext.getBean(EtcdConfigServiceImpl.BEAN_NAME, EtcdConfigServiceImpl.class);
    }
}
