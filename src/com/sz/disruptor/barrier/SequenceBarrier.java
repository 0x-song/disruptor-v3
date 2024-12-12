package com.sz.disruptor.barrier;

import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.WaitStrategy;

import java.util.Collections;
import java.util.List;

/**
 * @Author
 * @Date 2024-12-08 21:56
 * @Version 1.0
 */
public class SequenceBarrier {

    private final Sequence currentProducerSequence;

    private final WaitStrategy waitStrategy;

    private final List<Sequence> dependentSequencesList;

    public SequenceBarrier(Sequence currentProducerSequence, WaitStrategy waitStrategy, List<Sequence> dependentSequenceList) {
        this.currentProducerSequence = currentProducerSequence;
        this.waitStrategy = waitStrategy;
        if(!dependentSequenceList.isEmpty()){
            this.dependentSequencesList = dependentSequenceList;
        }else {
            //如果为空，则生产者序列号兜底，？？？？？？
            this.dependentSequencesList = Collections.singletonList(currentProducerSequence);
        }
    }

    public long getAvailableConsumerSequence(long currentConsumerSequence){
        return waitStrategy.waitFor(currentConsumerSequence, currentProducerSequence, dependentSequencesList);
    }
}
