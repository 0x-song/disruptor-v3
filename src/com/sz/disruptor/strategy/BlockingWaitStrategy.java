package com.sz.disruptor.strategy;

import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.util.SequenceUtils;
import com.sz.disruptor.util.ThreadUtils;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author
 * @Date 2024-12-09 22:20
 * @Version 1.0
 */
public class BlockingWaitStrategy implements WaitStrategy{

    private final Lock lock = new ReentrantLock();

    private final Condition processorNotifyCondition = lock.newCondition();


    @Override
    public long waitFor(long currentConsumerIndex, Sequence currentProducerSequence, List<Sequence> dependentSequenceList) {
        //生产者的序列小于当前消费者的序列，消费超前了
        if(currentProducerSequence.get() < currentConsumerIndex){
            lock.lock();
            try {
                while (currentProducerSequence.get() < currentConsumerIndex){
                    processorNotifyCondition.await();
                }
            }catch (Exception e){
            }finally {
                lock.unlock();
            }
        }
        //此时生产者序列已经超越了消费者序列
        long availableSequence;
        if(!dependentSequenceList.isEmpty()){
            //查找依赖的消费者最小序列
            while ((availableSequence = SequenceUtils.getMinimumSequence(dependentSequenceList)) < currentConsumerIndex){
                //当前消费者的消费速度 > 上游依赖的消费者速度
                //自旋
                ThreadUtils.onSpinWait();
            }
        }else {
            availableSequence = currentProducerSequence.get();
        }
        return availableSequence;
    }

    @Override
    public void signalWhenBlocking() {
        lock.lock();
        processorNotifyCondition.signalAll();
        lock.unlock();
    }
}
