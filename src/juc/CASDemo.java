package juc;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhanghao
 * @date 2020/5/5 - 14:55
 */
public class CASDemo {
    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger(5);
        System.out.println(atomicInteger.compareAndSet(5, 2020) + "\t current: "+atomicInteger.get());
        System.out.println(atomicInteger.compareAndSet(5, 2020) + "\t current: "+atomicInteger.get());
    }
}
