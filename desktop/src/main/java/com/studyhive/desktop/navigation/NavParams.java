package com.studyhive.desktop.navigation;

import java.util.HashMap;
import java.util.Map;

/** A small typed bag of navigation arguments (e.g. which community/material id to show). */
public final class NavParams {

    public static final NavParams EMPTY = new NavParams(Map.of());

    private final Map<String, Object> values;

    private NavParams(Map<String, Object> values) {
        this.values = values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getLong(String key) {
        Object v = values.get(key);
        return v == null ? null : ((Number) v).longValue();
    }

    public String getString(String key) {
        return (String) values.get(key);
    }

    public static final class Builder {
        private final Map<String, Object> map = new HashMap<>();

        public Builder put(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public NavParams build() {
            return new NavParams(map);
        }
    }
}
