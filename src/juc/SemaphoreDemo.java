package juc;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author zhanghao
 * @date 2020/5/6 - 10:30
 */
public class SemaphoreDemo {
    public static void main(String[] args) {
        // 模拟三个车位
        Semaphore semaphore = new Semaphore(3);
        // 模拟六辆车子抢占车位
        for (int i = 0; i < 6; i++){
            new Thread(() -> {
                try {
                    // 抢占资源
                    semaphore.acquire();
                    System.out.println(Thread.currentThread().getName()+"\t 抢到车位");
                    try { TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e) { e.printStackTrace(); }
                    System.out.println(Thread.currentThread().getName()+"\t 三秒后离开车位");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    // 释放资源
                    semaphore.release();
                }
            },String.valueOf(i)).start();
        }
    }
}
