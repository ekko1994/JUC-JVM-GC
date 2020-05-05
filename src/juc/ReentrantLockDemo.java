package juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhanghao
 * @date 2020/5/5 - 21:11
 */
class Phone implements Runnable{
    public synchronized void sendSMS()throws Exception {
        System.out.println(Thread.currentThread().getName()+"\t invoke sendSMS()");
        sendEmail();
    }

    private synchronized void sendEmail()throws Exception {
        System.out.println(Thread.currentThread().getName()+"\t ****invoke sendEmail()****");
    }

    //==================================================================================================================

    Lock lock = new ReentrantLock();
    @Override
    public void run() {
        get();
    }

    private void get() {
        lock.lock();
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName()+"\t invoke get()");
            set();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
            lock.unlock();
        }
    }

    private void set() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName()+"\t invoke set()");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}

/**
 * 运行结果:
 * t1	 invoke sendSMS()
 * t1	 ****invoke sendEmail()****
 * t2	 invoke sendSMS()
 * t2	 ****invoke sendEmail()****
 */
public class ReentrantLockDemo {

    public static void main(String[] args) {
        Phone phone = new Phone();

        new Thread(() -> {
            try {
                phone.sendSMS();
            } catch (Exception e) {
                e.printStackTrace();
            }
        },"t1").start();

        new Thread(() -> {
            try {
                phone.sendSMS();
            } catch (Exception e) {
                e.printStackTrace();
            }
        },"t2").start();

        try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        Thread t3 = new Thread(phone,"t3");
        Thread t4 = new Thread(phone,"t4");
        t3.start();
        t4.start();
    }
}
