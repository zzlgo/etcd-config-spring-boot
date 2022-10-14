package io.github.zzlgo.etcd.config.processor;

import io.github.zzlgo.etcd.config.EtcdConfigService;
import io.github.zzlgo.etcd.config.annotation.EtcdConfigurationProperties;
import io.github.zzlgo.etcd.config.component.EtcdConfigServiceImpl;
import io.github.zzlgo.etcd.config.event.UpdateReferenceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 实现{@link EtcdConfigurationProperties}的自动绑定及自动刷新
 *
 * @author zzl on 2020-03-23.
 */
public class EtcdConfigurationPropertiesBindingPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdConfigurationPropertiesBindingPostProcessor.class);

    public static final String BEAN_NAME = "etcdConfigurationPropertiesBindingPostProcessor";

    private ConfigurableApplicationContext applicationContext;
    private EtcdConfigService etcdConfigService;
    private AtomicBoolean addFlag = new AtomicBoolean(false);
    /**
     * 保存自动刷新的对象
     */
    private List<EtcdValueTarget> list = new ArrayList<>();


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        EtcdConfigurationProperties etcdConfigurationProperties = AnnotationUtils.findAnnotation(bean.getClass(), EtcdConfigurationProperties.class);

        if (etcdConfigurationProperties != null) {

            bind(bean, beanName, etcdConfigurationProperties);

            if (etcdConfigurationProperties.autoRefreshed()) {
                //自动刷新

                list.add(new EtcdValueTarget(bean, beanName, etcdConfigurationProperties));

                if (addFlag.compareAndSet(false, true)) {

                    etcdConfigService.addAllListener(new UpdateReferenceListener() {
                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            refresh();
                        }
                    });
                    LOG.info("add updateReferenceListener for @EtcdConfigurationProperties");
                }
            }
        }
        return bean;
    }

    /**
     * 刷新对象
     * 不知道具体哪个属性变了，全部刷新
     */
    private void refresh() {

        LOG.info("refresh for @EtcdConfigurationProperties, list size={}", list.size());
        for (EtcdValueTarget etcdValueTarget : list) {
            bind(etcdValueTarget.getBean(), etcdValueTarget.getBeanName(), etcdValueTarget.getEtcdConfigurationProperties());
        }
    }


    /**
     * 将属性值绑定到对象上
     *
     * @param bean
     * @param beanName
     * @param etcdConfigurationProperties
     */
    private void bind(Object bean, String beanName, EtcdConfigurationProperties etcdConfigurationProperties) {
        Binder binder = Binder.get(applicationContext.getEnvironment());
        ResolvableType type = ResolvableType.forClass(bean.getClass());
        Bindable<?> target = Bindable.of(type).withExistingValue(bean);
        binder.bind(etcdConfigurationProperties.prefix(), target, getBindHandler(etcdConfigurationProperties));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        this.etcdConfigService = applicationContext.getBean(EtcdConfigServiceImpl.BEAN_NAME, EtcdConfigServiceImpl.class);
    }

    private BindHandler getBindHandler(EtcdConfigurationProperties annotation) {
        BindHandler handler = new IgnoreTopLevelConverterNotFoundBindHandler();
        if (annotation.ignoreInvalidFields()) {
            handler = new IgnoreErrorsBindHandler(handler);
        }
        if (!annotation.ignoreUnknownFields()) {
            UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
            handler = new NoUnboundElementsBindHandler(handler, filter);
        }

        return handler;
    }

    private class EtcdValueTarget {

        private final Object bean;

        private final String beanName;

        private final EtcdConfigurationProperties etcdConfigurationProperties;

        public Object getBean() {
            return bean;
        }

        public String getBeanName() {
            return beanName;
        }

        public EtcdConfigurationProperties getEtcdConfigurationProperties() {
            return etcdConfigurationProperties;
        }

        public EtcdValueTarget(Object bean, String beanName, EtcdConfigurationProperties etcdConfigurationProperties) {
            this.bean = bean;
            this.beanName = beanName;
            this.etcdConfigurationProperties = etcdConfigurationProperties;
        }
    }
}
