package com.zzl.etcd.config.util;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author zzl on 2020-03-23.
 * @description
 */
public class BeanTypeConvertUtil {

    public static Object convertIfNecessary(ConfigurableListableBeanFactory beanFactory,
                                            Field field, Object value) {
        TypeConverter converter = beanFactory.getTypeConverter();
        return converter.convertIfNecessary(value, field.getType(), field);
    }

    public static Object convertIfNecessary(ConfigurableListableBeanFactory beanFactory,
                                            Method method, Object value) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] arguments = new Object[paramTypes.length];

        TypeConverter converter = beanFactory.getTypeConverter();

        if (arguments.length == 1) {
            return converter.convertIfNecessary(value, paramTypes[0],
                    new MethodParameter(method, 0));
        }

        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = converter.convertIfNecessary(value, paramTypes[i],
                    new MethodParameter(method, i));
        }

        return arguments;
    }

}
