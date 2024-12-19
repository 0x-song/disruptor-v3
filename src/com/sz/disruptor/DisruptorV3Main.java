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

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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

        Sequence[] workerSequences = workerPoolB.getCurrentWorkerSequences();
        ringBuffer.addGatingConsumerSequence(workerSequences);

        //通过依赖A创建序列屏障，创建消费者C
        SequenceBarrier sequenceBarrierC = ringBuffer.newBarrier(consumerSequenceA);
        BatchEventProcessor<OrderEventModel> eventProcessorC = new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerC"), sequenceBarrierC);
        Sequence consumerSequenceC = eventProcessorC.getCurrentConsumerSequence();
        ringBuffer.addGatingConsumerSequence(consumerSequenceC);

        //基于多线程消费者B和单线程消费者C来创建消费者D
        Sequence[] bcSequences = new Sequence[workerSequences.length + 1];
        System.arraycopy(workerSequences, 0, bcSequences, 0, workerSequences.length);
        bcSequences[bcSequences.length - 1] = consumerSequenceC;

        SequenceBarrier sequenceBarrierD = ringBuffer.newBarrier(bcSequences);
        BatchEventProcessor<OrderEventModel> eventProcessorD = new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerD"), sequenceBarrierD);
        Sequence consumerSequenceD = eventProcessorD.getCurrentConsumerSequence();
        ringBuffer.addGatingConsumerSequence(consumerSequenceD);


        //启动消费者A
        new Thread(eventProcessorA).start();

        //启动消费者B
        workerPoolB.start(Executors.newFixedThreadPool(10, new ThreadFactory() {

            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "worker" + threadNumber.getAndIncrement());
            }
        }));

        //启动消费者C
        new Thread(eventProcessorC).start();
        //启动消费者D
        new Thread(eventProcessorD).start();

        for (int i = 0; i < 100; i++) {
            long nextIndex = ringBuffer.next();
            OrderEventModel orderEventModel = ringBuffer.get(nextIndex);
            orderEventModel.setMessage("message" + i);
            orderEventModel.setPrice(i * 10);
            System.out.println("生产者发布事件:" + orderEventModel);
            ringBuffer.publish(nextIndex);
        }



    }
}
