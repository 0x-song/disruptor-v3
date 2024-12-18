package com.sz.disruptor;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.buffer.RingBuffer;
import com.sz.disruptor.event.OrderEventHandler;
import com.sz.disruptor.model.OrderEventModel;
import com.sz.disruptor.model.factory.OrderEventModelFactory;
import com.sz.disruptor.processor.BatchEventProcessor;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.BlockingWaitStrategy;
import com.sz.disruptor.worker.OrderWorkHandler;
import com.sz.disruptor.worker.WorkerPool;

/**
 * @Author
 * @Date 2024-12-18 21:58
 * @Version 1.0
 */
public class DisruptorV3Main {

    /**                     ------->多线程消费者B(依赖A)
     *  生产者--------消费者A                           ------->单线程消费者D(依赖B、C)
     *                      ------->单线程消费者C(依赖A)
     * @param args
     */
    public static void main(String[] args) {
        int ringBufferSize = 16;

        RingBuffer<OrderEventModel> ringBuffer = RingBuffer.createSingleProducer(new OrderEventModelFactory(),
                ringBufferSize,
                new BlockingWaitStrategy());

        //获取ringBuffer的序列屏障，只有生产者
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        //基于生产者序列屏障，创建消费者A
        BatchEventProcessor<OrderEventModel> eventProcessorA = new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerA"), sequenceBarrier);
        Sequence consumerSequenceA = eventProcessorA.getCurrentConsumerSequence();
        ringBuffer.addGatingConsumerSequence(consumerSequenceA);

        //通过依赖A创建消费者B
        SequenceBarrier workerSequenceBarrier = ringBuffer.newBarrier(consumerSequenceA);
        WorkerPool<OrderEventModel> workerPoolB = new WorkerPool<>(ringBuffer,
                workerSequenceBarrier,
                new OrderWorkHandler("workHandler1"),
                new OrderWorkHandler("workHandler2"),
                new OrderWorkHandler("workHandler3"));




    }
}
