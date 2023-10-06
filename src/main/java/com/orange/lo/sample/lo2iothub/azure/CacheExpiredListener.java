package com.orange.lo.sample.lo2iothub.azure;

public interface CacheExpiredListener {
    void onExpired(String key, LoMessageDetails value);
}
