package com.projectnil.api.queue;

public interface MessageListener<T> {
    void onMessage(T message);
}
