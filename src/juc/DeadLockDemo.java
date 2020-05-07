package juc;

import java.util.concurrent.TimeUnit;


class HoldLockThread implements Runnable {

    private String lockA;
    private String lockB;

    public HoldLockThread(String lockA, String lockB) {
        this.lockA = lockA;
        this.lockB = lockB;
    }

    @Override
    public void run() {
        synchronized (lockA){
            System.out.println(Thread.currentThread().getName()+"\t 自己持有: "+lockA + "\t 尝试获得: "+lockB);
            try { TimeUnit.SECONDS.sleep(2); } catch (InterruptedException e) { e.printStackTrace(); }
            synchronized (lockB){
                System.out.println(Thread.currentThread().getName()+"\t 自己持有: "+lockB + "\t 尝试获得: "+lockA);
            }
        }
    }
}
/**
 * @author zhanghao
 * @date 2020/5/7 - 17:21
 * 死锁是指两个或多个进程在执行过程中,
 * 由于竞争资源或者由于彼此通信而造成的一种阻塞的现象，
 * 若无外力作用，它们都将无法推进下去
 */
public class DeadLockDemo{
    public static void main(String[] args) {
        String lockA = "lockA";
        String lockB = "lockB";
        new Thread(new HoldLockThread(lockA,lockB),"AAA").start();
        new Thread(new HoldLockThread(lockB,lockA),"BBB").start();
    }
}