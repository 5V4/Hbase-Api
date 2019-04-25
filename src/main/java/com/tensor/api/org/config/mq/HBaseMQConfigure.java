package com.tensor.api.org.config.mq;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.tensor.api.org.config.schedule.Schedule;
import com.tensor.api.org.enpity.mq.Message;
import com.tensor.api.org.service.mq.ConsumerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class HBaseMQConfigure {

    @Value("${tensor.hbase-api.mq.ring-buffer-size}")
    private static int ringBufferSize;

    private static final ConcurrentHashMap<String, Disruptor<Message>> MQ = new ConcurrentHashMap<>();

    @Component
    @Scope(scopeName = "singleton")
    public static class HBaseMQ {

        private Disruptor<Message> disruptor(int ringBufferSize) {
            EventFactory<Message> factory = Message::new;
            return new Disruptor<>(factory, ringBufferSize, Schedule.MQ, ProducerType.SINGLE, new YieldingWaitStrategy());
        }

        public void register(String topic, ConsumerService consumerService) {
            if (MQ.containsKey(topic)) {
                throw new IllegalArgumentException(String.format("Topic-[%s] 已存在，不可重复注册[Topic]", topic));
            }
            Disruptor<Message> disruptor = disruptor(ringBufferSize);
            disruptor.handleEventsWithWorkerPool(consumerService);
            disruptor.start();
            MQ.put(topic, disruptor);
        }

    }

}