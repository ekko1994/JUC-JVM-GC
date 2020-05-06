package juc;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhanghao
 * @date 2020/5/6 - 14:10
 */
class ShareData {
    private int num = 0;
    Lock lock = new ReentrantLock();
    Condition condition = lock.newCondition();

    public void increment()throws Exception{
        lock.lock();
        try {
            // 判断
            while (0 != num){
                // 等待,不能生产消息
                condition.await();
            }
            // 干活
            num++;
            System.out.println(Thread.currentThread().getName()+"\t"+num);
            // 唤醒
            condition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void decrement()throws Exception{
        lock.lock();
        try {
            // 判断
            while (0 == num){
                // 等待,不能生产消息
                condition.await();
            }
            // 干活
            num--;
            System.out.println(Thread.currentThread().getName()+"\t"+num);
            // 唤醒
            condition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }
}

/**
 *  线程  操作  资源类
 *  判断  干活  唤醒
 *  防止虚假唤醒机制
 */
public class ProdConsumer_TraditionDemo {

    public static void main(String[] args) {
        ShareData shareData = new ShareData();

        new Thread(() -> {
            for (int i = 0; i <5 ; i++) {
                try {
                    shareData.increment();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },"AAA").start();
        
        new Thread(() -> {
            for (int i = 0; i <5 ; i++) {
                try {
                    shareData.decrement();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },"BBB").start();
    }
}
