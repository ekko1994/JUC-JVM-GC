package juc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhanghao
 * @date 2020/5/6 - 15:41
 */
class MyResource {
    // 默认开启,进行生产消费的交互
    private volatile boolean flag = true;
    private AtomicInteger atomicInteger = new AtomicInteger();
    private BlockingQueue<String> blockingQueue;

    public MyResource(BlockingQueue blockingQueue) {
        System.out.println("blockingQueue的类型是: "+blockingQueue.getClass().getName());
        this.blockingQueue = blockingQueue;
    }

    public void myProd() throws InterruptedException {
        boolean offer;
        String data = atomicInteger.incrementAndGet()+"";
        while (flag){
            offer = blockingQueue.offer(data, 2L, TimeUnit.SECONDS);
            if (offer){
                System.out.println(Thread.currentThread().getName()+"\t 插入队列"+data+"成功");
            }else{
                System.out.println(Thread.currentThread().getName()+"\t 插入队列"+data+"失败");
            }
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
        }
        System.out.println(Thread.currentThread().getName()+"\t flag = "+ flag + "不再生产!");
    }

    public void myConsumer() throws InterruptedException {
        String value = null;
        while (flag){
            value = blockingQueue.poll(2L, TimeUnit.SECONDS);
            if (null == value || "".equalsIgnoreCase(value)){
                flag = false;
                System.out.println(Thread.currentThread().getName()+"\t 超过两秒钟了,消费退出");
                return;
            }
            System.out.println(Thread.currentThread().getName()+"\t 消费成功");
        }
    }

    public void stop(){
        flag = false;
    }
}

public class ProdConsumerBlockQueueDemo {

    public static void main(String[] args) {
        MyResource myResource = new MyResource(new ArrayBlockingQueue(10));
        new Thread(() -> {
            System.out.println("生产者线程启动");
            try {
                myResource.myProd();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"prod").start();

        new Thread(() -> {
            System.out.println("消费者线程启动");
            try {
                myResource.myConsumer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"consumer").start();

        try { TimeUnit.SECONDS.sleep(5); } catch (InterruptedException e) { e.printStackTrace(); }
        myResource.stop();
        System.out.println(Thread.currentThread().getName()+"\t 超过五秒钟了停止生产消费!");
    }
}
