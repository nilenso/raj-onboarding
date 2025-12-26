package com.projectnil.common.domain.queue;

public interface MessagePublisher<T> {
    void publish(String queueName, T message);
}
