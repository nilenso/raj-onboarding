package com.projectnil.common.domain.queue;

public interface MessageListener<T> {
    void onMessage(T message);
}
