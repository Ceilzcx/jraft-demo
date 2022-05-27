package com.siesta.raft;

/**
 * @author hujiaofen
 * @since 27/5/2022
 */
public interface Lifecycle<T> {

    /**
     * initialize the service
     */
    boolean init(T options);

    void shutdown();
}