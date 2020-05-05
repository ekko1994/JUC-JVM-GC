package juc;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class MyData{

    volatile int number = 0;
//    int number = 0;
    public void addTo60(){
        this.number = 60;
    }

    // 此时number被volatile关键字修饰,volatile不保证原子性
    public void addPlusPlus(){
        number++;
    }

    AtomicInteger integer = new AtomicInteger();
    public void addMyAtomic(){
        integer.getAndIncrement();
    }

}

public class VolatileDemo {
    public static void main(String[] args) {
        // 验证volatile可见性
        //seeOKByVolatile();

        MyData myData = new MyData();

        for (int i = 0; i < 20; i++){
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    myData.addPlusPlus();
                    myData.addMyAtomic();
                }
            },String.valueOf(i)).start();
        }
        //等待上面20个线程全部计算完成后,再用main线程取得最终的结果值看是多少.
        while (Thread.activeCount() > 2){ // 2代表一个main线程和GC线程
            Thread.yield();
        }
        System.out.println(Thread.currentThread().getName()+"\t int type, finally number value: "+myData.number);
        System.out.println(Thread.currentThread().getName()+"\t AtomicInteger type, finally number value: "+myData.integer);
    }

    // volatile可以保证可见性,及时通知其他线程,主物理内存的值已经被修改.
    public static void seeOKByVolatile() {
        // 资源类
        MyData myData = new MyData();
        // A线程修改变量
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName()+"\t come in");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            myData.addTo60();
            System.out.println(Thread.currentThread().getName()+"\t update number value: "+myData.number);
        },"AAA").start();

        // 主线程就一直在这里等待循环,直到number的值不等于0
        while (myData.number == 0){

        }
        System.out.println(Thread.currentThread().getName()+"\t mission is over,main get number value: "+myData.number);
    }
}
