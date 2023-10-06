package com.orange.lo.sample.lo2iothub.azure;

import com.google.common.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessagesCache {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Cache<String, LoMessageDetails> cache;

    public MessagesCache(CacheExpiredListener expiredListener) {

        RemovalListener<String, LoMessageDetails> removalListener = notification -> {
            if (RemovalCause.EXPIRED == notification.getCause()){
                expiredListener.onExpired(notification.getKey(), notification.getValue());
            }
        };

        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .removalListener(removalListener)
                .build();

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            cache.cleanUp();
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void put(String key, LoMessageDetails value) {
        LOG.debug("Putting message {} to cache", key);
        cache.put(key, value);
    }

    public LoMessageDetails get(String key) {
        LOG.debug("Getting message {} from cache", key);
        return cache.getIfPresent(key);
    }

    public void invalidate(String key) {
        LOG.debug("Invalidating message {} from cache", key);
        cache.invalidate(key);
    }
}