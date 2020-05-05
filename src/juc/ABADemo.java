package juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * @author zhanghao
 * @date 2020/5/5 - 18:26
 */
public class ABADemo {

    private static AtomicReference<Integer> integerAtomicReference = new AtomicReference<>(100);

    private static AtomicStampedReference<Integer> stampedReference = new AtomicStampedReference<>(100,1);

    public static void main(String[] args) {
        System.out.println("-----以下是ABA问题的产生-----");
        new Thread(() -> {
            integerAtomicReference.compareAndSet(100,101);
            integerAtomicReference.compareAndSet(101,100);
        },"t1").start();

        new Thread(() -> {
            // 先暂停1秒 保证完成ABA
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
            System.out.println(integerAtomicReference.compareAndSet(100,2020)+"\t"+integerAtomicReference.get());
        },"t2").start();
        try { TimeUnit.SECONDS.sleep(2); } catch (InterruptedException e) { e.printStackTrace(); }
        System.out.println("-----以下是ABA问题的解决-----");

        new Thread(() -> {
            int stamp = stampedReference.getStamp();
            System.out.println(Thread.currentThread().getName()+"\t 第一次版本号: "+stamp);
            // 暂停1秒钟t3线程
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
            stampedReference.compareAndSet(100,101,stampedReference.getStamp(),stampedReference.getStamp()+1);
            System.out.println(Thread.currentThread().getName()+"\t 第二次版本号: "+stampedReference.getStamp());
            stampedReference.compareAndSet(101,100,stampedReference.getStamp(),stampedReference.getStamp()+1);
            System.out.println(Thread.currentThread().getName()+"\t 第三次版本号: "+stampedReference.getStamp());
        },"t3").start();

        new Thread(() -> {
            int stamp = stampedReference.getStamp();
            System.out.println(Thread.currentThread().getName()+"\t 第一次版本号: "+stamp + "\t 值是: "+stampedReference.getReference());
            // 暂停3秒钟t4线程,保证t3线程完成了一次ABA操作
            try { TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e) { e.printStackTrace(); }
            boolean result = stampedReference.compareAndSet(100, 2020, stamp, stamp + 1);
            System.out.println(Thread.currentThread().getName()+"\t 是否修改成功: "+result + "\t 最新版本号: "+stampedReference.getStamp());
            System.out.println(Thread.currentThread().getName()+"\t 最新值: "+stampedReference.getReference());

        },"t4").start();
    }
}
