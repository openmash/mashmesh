package com.sheepdog.mashmesh.util;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import java.util.Collections;

public class CacheProxy {
    private Cache cache;

    public CacheProxy() {
        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            cache = cacheFactory.createCache(Collections.emptyMap());
        } catch (CacheException e) {
            cache = null;
            // TODO: Log the exception
        }
    }

    public void put(Object key, Object value) {
        cache.put(key, value);
    }

    public Object get(Object key) {
        return cache.get(key);
    }
}
