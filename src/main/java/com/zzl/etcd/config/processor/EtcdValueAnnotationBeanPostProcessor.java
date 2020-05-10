package com.zzl.etcd.config.processor;

import com.zzl.etcd.config.EtcdConfigService;
import com.zzl.etcd.config.annotation.EtcdValue;
import com.zzl.etcd.config.component.EtcdConfigServiceImpl;
import com.zzl.etcd.config.event.UpdateReferenceListener;
import com.zzl.etcd.config.util.BeanTypeConvertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.core.BridgeMethodResolver.findBridgedMethod;
import static org.springframework.core.BridgeMethodResolver.isVisibilityBridgeMethodPair;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotation;
import static org.springframework.util.SystemPropertyUtils.*;

/**
 * 实现{@link EtcdValue}的自动注入及自动刷新
 *
 * @author zzl on 2020-03-20.
 * @description
 */
public class EtcdValueAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements MergedBeanDefinitionPostProcessor, BeanFactoryAware, EnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdValueAnnotationBeanPostProcessor.class);
    public static final String BEAN_NAME = "etcdValueAnnotationBeanPostProcessor";

    private ConfigurableListableBeanFactory beanFactory;
    private Environment environment;

    private final Class<EtcdValue> annotationType = EtcdValue.class;

    private final ConcurrentMap<String, AnnotatedInjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>();

    private AtomicBoolean addFlag = new AtomicBoolean(false);

    /**
     * 用于实现自动刷新。一个key，可能对应多个属性或方法
     */
    private Map<String, List<EtcdValueTarget>> placeholderEtcdValueTargetMap = new HashMap<>();
    private EtcdConfigService etcdConfigService;


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
        this.etcdConfigService = beanFactory.getBean(EtcdConfigServiceImpl.BEAN_NAME, EtcdConfigServiceImpl.class);
    }


    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        InjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
        metadata.checkConfigMembers(beanDefinition);
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        InjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (BeanCreationException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of @EtcdValue dependencies is failed", ex);
        }
        return pvs;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private InjectionMetadata findInjectionMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
        // Quick check on the concurrent map first, with minimal locking.
        AnnotatedInjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    try {
                        metadata = buildAnnotatedMetadata(clazz);
                        this.injectionMetadataCache.put(cacheKey, metadata);
                    } catch (NoClassDefFoundError err) {
                        throw new IllegalStateException("Failed to introspect object class [" + clazz.getName() +
                                "] for annotation metadata: could not find class that it depends on", err);
                    }
                }
            }
        }
        return metadata;
    }

    private class AnnotatedInjectionMetadata extends InjectionMetadata {

        private final Collection<AnnotatedFieldElement> fieldElements;

        private final Collection<AnnotatedMethodElement> methodElements;

        public AnnotatedInjectionMetadata(Class<?> targetClass, Collection<AnnotatedFieldElement> fieldElements,
                                          Collection<AnnotatedMethodElement> methodElements) {
            super(targetClass, combine(fieldElements, methodElements));
            this.fieldElements = fieldElements;
            this.methodElements = methodElements;
        }

        public Collection<AnnotatedFieldElement> getFieldElements() {
            return fieldElements;
        }

        public Collection<AnnotatedMethodElement> getMethodElements() {
            return methodElements;
        }
    }

    /**
     * {@link Method} {@link InjectionMetadata.InjectedElement}
     */
    private class AnnotatedMethodElement extends InjectionMetadata.InjectedElement {

        private final Method method;

        private final EtcdValue annotation;

        AnnotatedMethodElement(Method method, PropertyDescriptor pd, EtcdValue annotation) {
            super(method, pd);
            this.method = method;
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Class<?> injectedType = pd.getPropertyType();

            Object injectedObject = getInjectedObject(annotation, bean, beanName, injectedType, this);

            ReflectionUtils.makeAccessible(method);

            method.invoke(bean, injectedObject);

            if (annotation.autoRefreshed()) {
                //自动刷新
                saveAutoRefreshed(annotation.value(), bean, beanName, method, null);
            }

        }

    }


    /**
     * 保存自动刷新的句柄，用于实现自动刷新
     *
     * @param key
     * @param bean
     * @param beanName
     * @param method
     * @param field
     */
    private void saveAutoRefreshed(String key, Object bean, String beanName, Method method, Field field) {
        String value = beanFactory.resolveEmbeddedValue(key);
        //提前解析key
        key = resolvePlaceholder(key);
        EtcdValueTarget etcdValueTarget = new EtcdValueTarget(bean, beanName, method, field, value);

        List<EtcdValueTarget> valueList = placeholderEtcdValueTargetMap.get(key);
        if (valueList == null) {
            valueList = new ArrayList<>();
        }
        valueList.add(etcdValueTarget);
        placeholderEtcdValueTargetMap.put(key, valueList);

        if (addFlag.compareAndSet(false, true)) {
            etcdConfigService.addAllListener(new UpdateReferenceListener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    refresh();
                }
            });
            LOG.info("add updateReferenceListener for @EtcdValue");
        }
    }

    /**
     * 刷新对象
     * 此时environment中的属性已经刷新，但是不知道具体变化的是哪个key，所以只能全部判断
     */
    private void refresh() {
        try {
            LOG.info("refresh for @EtcdValue");
            for (Map.Entry<String, List<EtcdValueTarget>> entry : placeholderEtcdValueTargetMap.entrySet()) {

                /**
                 * 若key不提前解析
                 * 不可用environment.resolvePlaceholders()方法，当key被删除时，函数返回key本身，导致取到错误的value
                 * 不可用beanFactory.resolveEmbeddedValue()方法，当key被删除时，会报错
                 *
                 * 所以，采用提前解析key，environment查找的方式，这样当key被删除时，不会刷新原属性
                 */
                String newValue = environment.getProperty(entry.getKey());
                if (newValue == null) {
                    //配置key被删除
                    LOG.warn("config key is deleted,ignore refresh. key={}", entry.getKey());
                    continue;
                }
                List<EtcdValueTarget> beanPropertyList = entry.getValue();
                for (EtcdValueTarget target : beanPropertyList) {

                    if (!target.getLastValue().equals(newValue)) {
                        //不相同时才刷新，优化性能
                        target.setLastValue(newValue);
                        if (target.getMethod() == null) {
                            updateField(target, newValue);
                        } else {
                            updateMethod(target, newValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        }
    }


    private void updateField(EtcdValueTarget etcdValueTarget, String propertyValue) {
        Object bean = etcdValueTarget.bean;
        Field field = etcdValueTarget.field;
        try {
            ReflectionUtils.makeAccessible(field);
            field.set(bean, BeanTypeConvertUtil.convertIfNecessary(beanFactory, field, propertyValue));

            LOG.info("updateField beanName={},field={}", etcdValueTarget.getBeanName(), field.getName());

        } catch (Throwable e) {
            LOG.error("updateField", e);
        }
    }

    private void updateMethod(EtcdValueTarget etcdValueTarget, String propertyValue) {
        Method method = etcdValueTarget.method;
        ReflectionUtils.makeAccessible(method);
        try {
            method.invoke(etcdValueTarget.bean, BeanTypeConvertUtil.convertIfNecessary(beanFactory, method, propertyValue));
            LOG.info("updateMethod beanName={},method={}", etcdValueTarget.getBeanName(), method.getName());
        } catch (Throwable e) {
            LOG.error("updateMethod", e);
        }
    }

    /**
     * {@link Field} {@link InjectionMetadata.InjectedElement}
     */
    public class AnnotatedFieldElement extends InjectionMetadata.InjectedElement {

        private final Field field;

        private final EtcdValue annotation;

        AnnotatedFieldElement(Field field, EtcdValue annotation) {
            super(field, null);
            this.field = field;
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Class<?> injectedType = field.getType();

            Object injectedObject = getInjectedObject(annotation, bean, beanName, injectedType, this);

            ReflectionUtils.makeAccessible(field);

            field.set(bean, injectedObject);

            if (annotation.autoRefreshed()) {
                saveAutoRefreshed(annotation.value(), bean, beanName, null, field);
            }

        }

    }


    /**
     * 查找需要注入的对象
     *
     * @param annotation
     * @param bean
     * @param beanName
     * @param injectedType
     * @param injectedElement
     * @return
     */
    private Object getInjectedObject(EtcdValue annotation, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        String annotationValue = annotation.value();
        //查找不到，会报错，启动失败
        String value = beanFactory.resolveEmbeddedValue(annotationValue);

        Member member = injectedElement.getMember();
        if (member instanceof Field) {
            return BeanTypeConvertUtil.convertIfNecessary(beanFactory, (Field) member, value);
        }

        if (member instanceof Method) {
            return BeanTypeConvertUtil.convertIfNecessary(beanFactory, (Method) member, value);
        }
        return null;
    }

    private static <T> Collection<T> combine(Collection<? extends T>... elements) {
        List<T> allElements = new ArrayList<T>();
        for (Collection<? extends T> e : elements) {
            allElements.addAll(e);
        }
        return allElements;
    }

    private EtcdValueAnnotationBeanPostProcessor.AnnotatedInjectionMetadata buildAnnotatedMetadata(final Class<?> beanClass) {
        Collection<AnnotatedFieldElement> fieldElements = findAnnotatedFieldMetadata(beanClass);
        Collection<AnnotatedMethodElement> methodElements = findAnnotatedMethodMetadata(beanClass);
        return new EtcdValueAnnotationBeanPostProcessor.AnnotatedInjectionMetadata(beanClass, fieldElements, methodElements);
    }


    /**
     * Finds {@link InjectionMetadata.InjectedElement} Metadata from annotated {@link EtcdValue} fields
     *
     * @param beanClass The {@link Class} of Bean
     * @return non-null {@link List}
     */
    private List<AnnotatedFieldElement> findAnnotatedFieldMetadata(final Class<?> beanClass) {

        final List<AnnotatedFieldElement> elements = new LinkedList<AnnotatedFieldElement>();

        ReflectionUtils.doWithFields(beanClass, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

                EtcdValue annotation = getAnnotation(field, annotationType);

                if (annotation != null) {

                    if (Modifier.isStatic(field.getModifiers())) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("@" + annotationType.getName() + " is not supported on static fields: " + field);
                        }
                        return;
                    }

                    elements.add(new EtcdValueAnnotationBeanPostProcessor.AnnotatedFieldElement(field, annotation));
                }

            }
        });

        return elements;

    }

    /**
     * Finds {@link InjectionMetadata.InjectedElement} Metadata from annotated {@link EtcdValue} methods
     *
     * @param beanClass The {@link Class} of Bean
     * @return non-null {@link List}
     */
    private List<AnnotatedMethodElement> findAnnotatedMethodMetadata(final Class<?> beanClass) {

        final List<AnnotatedMethodElement> elements = new LinkedList<AnnotatedMethodElement>();

        ReflectionUtils.doWithMethods(beanClass, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {

                Method bridgedMethod = findBridgedMethod(method);

                if (!isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                    return;
                }

                EtcdValue annotation = findAnnotation(bridgedMethod, annotationType);

                if (annotation != null && method.equals(ClassUtils.getMostSpecificMethod(method, beanClass))) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("@" + annotationType.getSimpleName() + " annotation is not supported on static methods: " + method);
                        }
                        return;
                    }
                    if (method.getParameterTypes().length == 0) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("@" + annotationType.getSimpleName() + " annotation should only be used on methods with parameters: " +
                                    method);
                        }
                    }
                    PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, beanClass);
                    elements.add(new EtcdValueAnnotationBeanPostProcessor.AnnotatedMethodElement(method, pd, annotation));
                }
            }
        });

        return elements;

    }


    private class EtcdValueTarget {

        private final Object bean;

        private final String beanName;

        private final Method method;

        private final Field field;

        //上次的值，用于判断value是否变化
        private String lastValue = "";


        public Object getBean() {
            return bean;
        }

        public String getBeanName() {
            return beanName;
        }

        public Method getMethod() {
            return method;
        }

        public Field getField() {
            return field;
        }

        public String getLastValue() {
            return lastValue;
        }

        public void setLastValue(String lastValue) {
            this.lastValue = lastValue;
        }

        public EtcdValueTarget(Object bean, String beanName, Method method, Field field, String value) {
            this.bean = bean;
            this.beanName = beanName;
            this.method = method;
            this.field = field;
            this.lastValue = value;
        }

    }


    /**
     * 去掉${}
     *
     * @param placeholder
     * @return
     */
    private String resolvePlaceholder(String placeholder) {
        if (!placeholder.startsWith(PLACEHOLDER_PREFIX)) {
            return null;
        }

        if (!placeholder.endsWith(PLACEHOLDER_SUFFIX)) {
            return null;
        }

        if (placeholder.length() <= PLACEHOLDER_PREFIX.length()
                + PLACEHOLDER_SUFFIX.length()) {
            return null;
        }

        int beginIndex = PLACEHOLDER_PREFIX.length();
        int endIndex = placeholder.length() - PLACEHOLDER_PREFIX.length() + 1;
        placeholder = placeholder.substring(beginIndex, endIndex);

        int separatorIndex = placeholder.indexOf(VALUE_SEPARATOR);
        if (separatorIndex != -1) {
            return placeholder.substring(0, separatorIndex);
        }

        return placeholder;
    }


}
