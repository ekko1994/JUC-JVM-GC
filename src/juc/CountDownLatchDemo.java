package juc;

import java.util.concurrent.CountDownLatch;

/**
 * @author zhanghao
 * @date 2020/5/6 - 9:08
 */
public class CountDownLatchDemo {

    public static void main(String[] args) throws Exception {
//        leaveClassRoom();
        country();
    }

    private static void country() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(6);
        for (int i = 1; i < 7; i++){
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName()+"\t 国,被灭");
                countDownLatch.countDown();
            }, CountryEnum.foreach_CountryEnum(i).getRetMessage()).start();
        }
        countDownLatch.await();
        System.out.println(Thread.currentThread().getName()+"\t 秦国大一统");
    }

    private static void leaveClassRoom() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(6);
        for (int i = 0; i < 6; i++){
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName()+"\t 上完自习,离开教室");
                countDownLatch.countDown();
            },String.valueOf(i)).start();
        }

        countDownLatch.await();
        System.out.println(Thread.currentThread().getName()+"\t 班长关门离开教室");
    }
}
