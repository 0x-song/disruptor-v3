package com.sz.disruptor.sequence;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.strategy.WaitStrategy;
import com.sz.disruptor.util.SequenceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * @Author
 * @Date 2024-12-08 21:23
 * @Version 1.0
 * 单线程生产者序号生成器
 */
public class SingleProducerSequencer {

    private final int ringBufferSize;

    private final Sequence currentProducerSequence = new Sequence();

    //消费者序列器
    private final List<Sequence> gatingConsumerSequenceList = new ArrayList<Sequence>();

    private final WaitStrategy waitStrategy;

    private long nextValue = -1;

    private long cachedConsumerSequenceValue = -1;

    public SingleProducerSequencer(int ringBufferSize, WaitStrategy waitStrategy) {
        this.ringBufferSize = ringBufferSize;
        this.waitStrategy = waitStrategy;
    }

    public long next(){
        return next(1);
    }

    //一次申请n个生产者序列号
    public long next(int n){
        long nextProducerSequence = this.nextValue + n;

        //是否超过消费者一圈的临界点序列
        long wrapPoint = nextProducerSequence - this.ringBufferSize;

        long cachedGatingSequence = this.cachedConsumerSequenceValue;

        if(wrapPoint > cachedGatingSequence){
            long minSequence;
            while (wrapPoint > (minSequence = SequenceUtils.getMinimumSequence(nextProducerSequence, gatingConsumerSequenceList))){
                LockSupport.parkNanos(1L);
            }

            this.cachedConsumerSequenceValue = minSequence;
        }

        this.nextValue = nextProducerSequence;
        return nextProducerSequence;
    }

    public void publish(long publishIndex){
        this.currentProducerSequence.lazySet(publishIndex);

        this.waitStrategy.signalWhenBlocking();
    }

    public SequenceBarrier newBarrier(){
        return new SequenceBarrier(this.currentProducerSequence, this.waitStrategy, new ArrayList<>());
    }

    public SequenceBarrier newBarrier(Sequence... dependenceSequences){
        return new SequenceBarrier(this.currentProducerSequence, this.waitStrategy, new ArrayList<>(Arrays.asList(dependenceSequences)));
    }

    public void addGatingConsumerSequenceList(Sequence gatingConsumerSequence){
        this.gatingConsumerSequenceList.add(gatingConsumerSequence);
    }

    public int getRingBufferSize() {
        return ringBufferSize;
    }

}
