package com.demo.job.admin.core.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalCacheUtil {

    private static ConcurrentMap<String, LocalCacheData> cacheRepository = new ConcurrentHashMap<>();

    private static class LocalCacheData {
        private String key;
        private Object value;
        private long timeoutTime;

        public LocalCacheData() {

        }

        public LocalCacheData(String key, Object value, long timeoutTime) {
            this.key = key;
            this.value = value;
            this.timeoutTime = timeoutTime;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public long getTimeoutTime() {
            return timeoutTime;
        }

        public void setTimeoutTime(long timeoutTime) {
            this.timeoutTime = timeoutTime;
        }
    }

    public static LocalCacheData get(String key) {
        if(key == null || key.trim().length() == 0) {
            return null;
        }
        LocalCacheData localCacheData = cacheRepository.get(key);
        if(localCacheData != null && System.currentTimeMillis() < localCacheData.getTimeoutTime()) {
            return localCacheData;
        } else {
            remove(key);
            return null;
        }
    }

    public static boolean remove(String key) {
        if(key == null || key.trim().length() == 0) {
            return false;
        }
        return cacheRepository.remove(key)!= null;
    }

    public static boolean cleanTimeoutCache() {
        return cacheRepository.values().removeIf(localCacheData -> System.currentTimeMillis() > localCacheData.getTimeoutTime());
    }
}
