package jvm;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * @author zhanghao
 * @date 2020/5/8 - 15:28
 */
public class PhantomReferenceDemo {
    public static void main(String[] args) {
        Object o = new Object();
        ReferenceQueue referenceQueue = new ReferenceQueue();
        PhantomReference<Object> phantomReference = new PhantomReference<>(o,referenceQueue);

        System.out.println(o);
        System.out.println(phantomReference.get());
        System.out.println(referenceQueue.poll());

        System.out.println("**************************");
        o = null;
        System.gc();
        System.out.println(o);
        System.out.println(phantomReference.get());
        System.out.println(referenceQueue.poll());

    }
}
