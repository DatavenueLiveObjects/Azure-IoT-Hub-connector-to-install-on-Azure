package com.orange.lo.sample.lo2iothub.utils;

import com.microsoft.azure.sdk.iot.device.Message;
import org.springframework.stereotype.Service;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;

@Service
public class CacheService {

    public static final String MESSAGES_CACHE_KEY = "MESSAGES_CACHE";
    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        configureAndCreateCache();
    }

    private void configureAndCreateCache() {
        MutableConfiguration<String, Message> config
                = new MutableConfiguration<>();
        cacheManager.createCache(MESSAGES_CACHE_KEY, config);
    }

    public void x(String key, Message message) {
        Cache<String, Message> cache = getMessageCache();
        cache.put(key, message);
    }

    private void r(String key) {
        Cache<String, Message> cache = getMessageCache();
        cache.remove(key);
    }

    private Cache<String, Message> getMessageCache() {
        return cacheManager.getCache(MESSAGES_CACHE_KEY);
    }
}
