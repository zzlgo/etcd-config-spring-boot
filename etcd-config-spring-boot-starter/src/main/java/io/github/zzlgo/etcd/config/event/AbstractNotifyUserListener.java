package io.github.zzlgo.etcd.config.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通知用户自定义回调方法
 *
 * @author zzl on 2020-03-24.
 */
public abstract class AbstractNotifyUserListener implements Listener {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNotifyUserListener.class);

    private String dataId;
    private long timeout;

    protected AbstractNotifyUserListener(String dataId, long timeout) {
        this.dataId = dataId;
        this.timeout = timeout;
    }

    private static AtomicInteger id = new AtomicInteger(0);
    private static ExecutorService executorService = Executors.newScheduledThreadPool(3,
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setName("etcd-notifyUserListener-" + id.incrementAndGet());
                    return t;
                }
            });


    @Override
    public void receiveConfigInfo(String configInfo) {

        Future future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                onReceived(configInfo);
            }
        });
        try {
            future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            future.cancel(true);
            LOG.warn("Listening exceeds timeout, dataId={}, {} ms", dataId, timeout);
        }
    }


    protected abstract void onReceived(String configInfo);
}
