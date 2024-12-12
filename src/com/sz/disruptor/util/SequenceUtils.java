package com.sz.disruptor.util;

import com.sz.disruptor.sequence.Sequence;

import java.util.List;

/**
 * @Author
 * @Date 2024-12-09 21:32
 * @Version 1.0
 */
public class SequenceUtils {

    /**
     *
     * @param minimumSequence 申请的最小序列
     * @param gatingConsumerSequenceList 多个消费者，要不快于最慢的那个
     * @return
     */
    public static long getMinimumSequence(long minimumSequence, List<Sequence> gatingConsumerSequenceList) {
        for (Sequence sequence : gatingConsumerSequenceList) {
            long value = sequence.get();
            minimumSequence = Math.min(minimumSequence, value);
        }
        return minimumSequence;
    }

    public static long getMinimumSequence(List<Sequence> gatingConsumerSequenceList) {
        return getMinimumSequence(Long.MAX_VALUE, gatingConsumerSequenceList);
    }
}
