package com.siesta.raft.exception;

// SPI相关自定义异常类
public class ExtensionException extends Exception {

    public ExtensionException(String msg) {
        super(msg);
    }

}
