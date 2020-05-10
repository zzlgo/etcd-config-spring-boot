package com.zzl.etcd.config.component;

import com.zzl.etcd.config.EtcdConfigService;
import com.zzl.etcd.config.event.AbstractNotifyUserListener;
import com.zzl.etcd.config.event.Listener;
import com.zzl.etcd.config.event.UpdateCacheListener;
import com.zzl.etcd.config.event.UpdateReferenceListener;
import com.zzl.etcd.config.exception.EtcdConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author zzl on 2020-03-20.
 * @description
 */
public class EtcdConfigServiceImpl implements EtcdConfigService, ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdConfigServiceImpl.class);

    public static final String BEAN_NAME = "etcdConfigServiceImpl";

    private EtcdComponent etcdComponent;
    /**
     * 保存所有的listener
     */
    private final AtomicReference<Map<String, CopyOnWriteArrayList<Listener>>> cacheMap = new AtomicReference<>(new HashMap<>());
    /**
     * 代表所有dataId
     */
    private static final String ALL_DATA_ID = "*";

    @Override
    public String getConfig(String dataId) {
        try {
            return etcdComponent.getValue(dataId);
        } catch (Exception e) {
            throw new EtcdConfigException("getConfig", e);
        }

    }

    @Override
    public void addListener(String dataId, Listener listener) {

        registerWatchAndCacheListener(dataId, listener);

    }

    @Override
    public void addAllListener(Listener listener) {

        registerWatchAndCacheListener(ALL_DATA_ID, listener);
    }


    /**
     * listener按order分为三类，按order排序为
     * {@link UpdateCacheListener}
     * {@link UpdateReferenceListener}
     * {@link AbstractNotifyUserListener}
     * <p>
     * listener按scope分为两类，
     * dataId监听与所有dataId监听
     *
     * @param dataId
     * @param configInfo
     */
    private void invokeEtcdListener(String dataId, String configInfo) {

        try {
            CopyOnWriteArrayList<Listener> listeners1 = cacheMap.get().get(dataId);
            CopyOnWriteArrayList<Listener> listeners2 = cacheMap.get().get(ALL_DATA_ID);
            List<Listener> listenerList = combine(listeners1, listeners2);
            if (listenerList.size() == 0) {
                return;
            }
            //将listener分类，按顺序执行
            List<Listener> updateCacheListener = getUpdateCacheListener(listenerList);
            List<Listener> updateReferenceListener = getUpdateReferenceListener(listenerList);
            List<Listener> notifyUserListener = getNotifyUserListener(listenerList);
            invoke(updateCacheListener, configInfo);
            invoke(updateReferenceListener, configInfo);
            invoke(notifyUserListener, configInfo);
        } catch (Exception e) {
            LOG.error("", e);
        }

    }

    private void invoke(List<Listener> listeners, String configInfo) {
        for (Listener listener : listeners) {
            listener.receiveConfigInfo(configInfo);
        }
    }

    /**
     * 注册watch，保存所有的listener
     *
     * @param dataId
     * @param listener
     */
    private void registerWatchAndCacheListener(String dataId, Listener listener) {
        CopyOnWriteArrayList<Listener> listeners = getListAndRegisterEtcdWatchIfAbsent(dataId);
        listeners.add(listener);
    }


    private CopyOnWriteArrayList<Listener> getListAndRegisterEtcdWatchIfAbsent(String dataId) {

        CopyOnWriteArrayList<Listener> listeners = cacheMap.get().get(dataId);
        if (listeners == null) {
            synchronized (cacheMap) {
                listeners = cacheMap.get().get(dataId);
                if (listeners == null) {
                    listeners = new CopyOnWriteArrayList<>();

                    Map<String, CopyOnWriteArrayList<Listener>> copy = cacheMap.get();
                    copy.put(dataId, listeners);
                    cacheMap.set(copy);

                    registerEtcdWatch(dataId);
                }
            }
        }
        return listeners;
    }


    /**
     * 每个dataId只会向etcd注册一次
     *
     * @param dataId
     */
    private void registerEtcdWatch(String dataId) {
        if (!ALL_DATA_ID.equals(dataId)) {
            //注册etcd watch
            etcdComponent.watch(dataId, new Consumer<String>() {
                @Override
                public void accept(String s) {
                    invokeEtcdListener(dataId, s);
                }
            });
            LOG.info("registerEtcdWatch dataId={}", dataId);
        }
    }

    private List<Listener> combine(CopyOnWriteArrayList<Listener> listeners1, CopyOnWriteArrayList<Listener> listeners2) {
        List<Listener> list = new ArrayList<>();
        if (listeners1 != null) {
            list.addAll(listeners1);
        }
        if (listeners2 != null) {
            list.addAll(listeners2);
        }
        return list;
    }

    private List<Listener> getNotifyUserListener(List<Listener> listenerList) {
        List<Listener> list = new ArrayList<>();
        for (Listener listener : listenerList) {
            if (listener instanceof AbstractNotifyUserListener) {
                list.add(listener);
            }
        }
        return list;
    }

    private List<Listener> getUpdateReferenceListener(List<Listener> listenerList) {
        List<Listener> list = new ArrayList<>();
        for (Listener listener : listenerList) {
            if (listener instanceof UpdateReferenceListener) {
                list.add(listener);
            }
        }
        return list;
    }

    private List<Listener> getUpdateCacheListener(List<Listener> listenerList) {
        List<Listener> list = new ArrayList<>();
        for (Listener listener : listenerList) {
            if (listener instanceof UpdateCacheListener) {
                list.add(listener);
            }
        }
        return list;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.etcdComponent = applicationContext.getBean(EtcdComponent.BEAN_NAME, EtcdComponent.class);
    }
}
