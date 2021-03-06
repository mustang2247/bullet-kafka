/*
 *  Copyright 2017, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.kafka;

import com.yahoo.bullet.operations.SerializerDeserializer;
import com.yahoo.bullet.pubsub.BufferingSubscriber;
import com.yahoo.bullet.pubsub.PubSubException;
import com.yahoo.bullet.pubsub.PubSubMessage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KafkaSubscriber extends BufferingSubscriber {
    @Getter(AccessLevel.PACKAGE)
    private KafkaConsumer<String, byte[]> consumer;

    /**
     * Creates a KafkaSubscriber using a {@link KafkaConsumer}.
     *
     * @param consumer The {@link KafkaConsumer} to read data from.
     * @param maxUnackedMessages The maximum number of messages that can be received before an ack is needed.
     */
    public KafkaSubscriber(KafkaConsumer<String, byte[]> consumer, int maxUnackedMessages) {
        super(maxUnackedMessages);
        this.consumer = consumer;
    }

    @Override
    public List<PubSubMessage> getMessages() throws PubSubException {
        consumer.commitSync();
        ConsumerRecords<String, byte[]> buffer;
        try {
            buffer = consumer.poll(0);
        } catch (KafkaException e) {
            throw new PubSubException("Consumer poll failed", e);
        }
        List<PubSubMessage> messages = new ArrayList<>();
        for (ConsumerRecord<String, byte[]> record : buffer) {
            Object message = SerializerDeserializer.fromBytes(record.value());
            if (message == null || !(message instanceof PubSubMessage)) {
                log.warn("Invalid message received: {}", message);
                continue;
            }
            messages.add((PubSubMessage) message);
        }
        return messages;
    }

    @Override
    public void close() {
        consumer.close();
    }
}
