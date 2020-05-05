package juc;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.TagName;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author zhanghao
 * @date 2020/5/5 - 22:09
 */
class MyChache{

    private volatile Map<String, Object> map = new HashMap<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void put(String key, Object value){
        lock.writeLock().lock();
        try {
            System.out.println(Thread.currentThread().getName()+"\t 正在写入: "+key);
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
            map.put(key, value);
            System.out.println(Thread.currentThread().getName()+"\t 写入完成: ");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void get(String key){
        lock.readLock().lock();
        try {
            System.out.println(Thread.currentThread().getName()+"\t 正在读取: ");
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
            Object o = map.get(key);
            System.out.println(Thread.currentThread().getName()+"\t 读取完成: "+o);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }
}

/**
 *  读-读可共存
 *  读-写不能共存
 *  写-写不能共存
 *
 *  写操作: 原子+独占,整个过程必须是一个完整的统一体,中间不许被分割,被打断
 */
public class ReadWriteLockDemo {
    public static void main(String[] args) {
        MyChache myChache = new MyChache();

        for (int i = 0; i < 5; i++){
            final int tempInt = i;
            new Thread(() -> {
                myChache.put(tempInt+"",tempInt+"");
            },String.valueOf(i)).start();
        }

        for (int i = 0; i < 5; i++){
            int finalI = i;
            new Thread(() -> {
                myChache.get(finalI +"");
            },String.valueOf(i)).start();
        }
    }
}
