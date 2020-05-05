package juc;

/**
 * @author zhanghao
 * @date 2020/5/5 - 14:11
 */
public class SingletonDemo {

    private static volatile SingletonDemo instance = null;

    private SingletonDemo() {
        System.out.println(Thread.currentThread().getName()+"\t 我是构造方法SingletonDemo()");
    }

    public static SingletonDemo getInstance(){
        if (instance == null) {
            //DCL模式 Double Check Lock 双端检索机制：在加锁前后都进行判断
            synchronized (SingletonDemo.class) {
                if (instance == null) {
                    instance = new SingletonDemo();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++){
            new Thread(() -> {
                SingletonDemo.getInstance();
            },String.valueOf(i)).start();
        }
    }
}
