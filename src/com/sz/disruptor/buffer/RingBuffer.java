package com.sz.disruptor.buffer;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.model.factory.EventModelFactory;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.sequence.SingleProducerSequencer;
import com.sz.disruptor.strategy.WaitStrategy;

/**
 * @Author
 * @Date 2024-12-10 21:38
 * @Version 1.0
 */
public class RingBuffer<T> {

    private final T[] elementList;

    private final SingleProducerSequencer singleProducerSequencer;

    private final int ringBufferSize;

    private final int mask;

    public RingBuffer(SingleProducerSequencer singleProducerSequencer, EventModelFactory<T> eventModelFactory){
        int bufferSize = singleProducerSequencer.getRingBufferSize();
        if(Integer.bitCount(bufferSize) != 1){
            throw new IllegalArgumentException("Buffer size must be a power of 2");
        }
        this.singleProducerSequencer = singleProducerSequencer;
        this.ringBufferSize = bufferSize;
        this.mask = bufferSize - 1;
        this.elementList = (T[]) new Object[ringBufferSize];
        for (int i = 0; i < elementList.length; i++) {
            this.elementList[i] = eventModelFactory.newInstance();
        }
    }

    public T get(long sequence) {
        int index = (int) (sequence & mask);
        return elementList[index];
    }

    public long next(){
        return singleProducerSequencer.next();
    }

    public long next(int n){
        return singleProducerSequencer.next(n);
    }

    public void publish(long index){
        singleProducerSequencer.publish(index);
    }

    public void addGatingConsumerSequence(Sequence consumerSequence){
        singleProducerSequencer.addGatingConsumerSequenceList(consumerSequence);
    }

    public SequenceBarrier newBarrier(){
        return singleProducerSequencer.newBarrier();
    }

    public SequenceBarrier newBarrier(Sequence... dependentSequences){
        return singleProducerSequencer.newBarrier(dependentSequences);
    }

    public static <E> RingBuffer<E> createSingleProducer(EventModelFactory<E> eventModelFactory, int bufferSize, WaitStrategy waitStrategy){
        SingleProducerSequencer singleProducerSequencer = new SingleProducerSequencer(bufferSize, waitStrategy);
        return new RingBuffer<E>(singleProducerSequencer, eventModelFactory);
    }

    public Sequence getCurrentProducerSequencer() {
        return singleProducerSequencer.getCurrentProducerSequence();
    }
}
