package com.projectnil.api.queue;

public interface MessagePublisher<T> {
    void publish(String queueName, T message);
}
