package com.sz.disruptor.strategy;

import com.sz.disruptor.sequence.Sequence;

import java.util.List;

/**
 * @Author
 * @Date 2024-12-08 21:55
 * @Version 1.0
 */
public interface WaitStrategy {

    long waitFor(long currentConsumerIndex, Sequence currentProducerSequence, List<Sequence> dependentSequenceList);

    void signalWhenBlocking();

}
