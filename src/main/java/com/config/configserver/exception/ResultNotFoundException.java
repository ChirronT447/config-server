package com.config.configserver.exception;

public class ResultNotFoundException extends Exception {

    public ResultNotFoundException(String key) {
        super(key);
    }
}
