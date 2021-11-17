package com.config.configserver.database;

public record Row(
        String APPLICATION,
        String PROFILE,
        String PROP_KEY,
        String VALUE,
        String LABEL) {}
