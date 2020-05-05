# JUC多线程及并发包

## 1. 谈谈你对volatile的理解

### 1. volatile是Java虚拟机提供的轻量级的同步机制

- 保证可见性
- 不保证原子性
- 禁止指令重排

### 2. JMM你谈谈

JMM(Java内存模型Java Memory Model,简称JMM)本身是一种抽象的概念 并不真实存在,它描述的是一组规则或规范通过规范定制了程序中各个变量(包括实例字段,静态字段和构成数组对象的元素)的访问方式.

JMM关于同步规定:

- 线程解锁前,必须把共享变量的值刷新回主内存
- 线程加锁前,必须读取主内存的最新值到自己的工作内存
- 加锁解锁是同一把锁

由于JVM运行程序的实体是线程,而每个线程创建时JVM都会为其创建一个工作内存(有些地方成为栈空间),工作内存是每个线程的私有数据区域,而Java内存模型中规定所有变量都存储在主内存,主内存是共享内存区域,所有线程都可访问,但线程对变量的操作(读取赋值等)必须在工作内存中进行,首先要将变量从主内存拷贝到自己的工作空间,然后对变量进行操作,操作完成再将变量写回主内存,不能直接操作主内存中的变量,各个线程中的工作内存储存着主内存中的变量副本拷贝,因此不同的线程无法访问对方的工作内存,线程间的通讯(传值) 必须通过主内存来完成,其简要访问过程如下图:

![JMM](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/JMM.jpg)

#### 2.1 可见性

> 通过前面对JMM的介绍,我们知道: 
> 各个线程对主内存中共享变量的操作都是各个线程各自拷贝到自己的工作内存操作后再写回主内存中的.
> 这就可能存在一个线程AAA修改了共享变量X的值还未写回主内存中时 ,另外一个线程BBB又对内存中的一个共享变量X进行操作,但此时A线程工作内存中的共享变量X对线程B来说并不可见.这种工作内存与主内存同步延迟现象就造成了可见性问题.

```java
class MyData{

//    volatile int number = 0;
    int number = 0;
    public void addTo60(){
        this.number = 60;
    }
}

public class VolatileDemo {
    public static void main(String[] args) {
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
```

`MyData`中的number没有`volatile`修饰时的运行结果: 

```java
AAA	 come in
AAA	 update number value: 60
```

AAA线程将number的值改为了60,但是main线程的本地内存的number的值仍然是0,所以main线程一直在循环

`MyData`中的number被`volatile`修饰时的运行结果: 

```java
AAA	 come in
AAA	 update number value: 60
main	 mission is over,main get number value: 60
```

加了`volatile`修饰的变量number,在AAA线程修改为60后,会将本地内存的number值刷新到主内存中,使得main线程获取到了最新的值,所以`volatile`可以保证可见性.

#### 2.2 原子性

```java
class MyData{

    volatile int number = 0;
    public void addTo60(){
        this.number = 60;
    }

    // 此时number被volatile关键字修饰,volatile不保证原子性
    public void addPlusPlus(){
        number++;
    }
}

public class VolatileDemo {
    public static void main(String[] args) {
        MyData myData = new MyData();
        
        for (int i = 0; i < 20; i++){
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    myData.addPlusPlus();
                }
            },String.valueOf(i)).start();
        }
        //等待上面20个线程全部计算完成后,再用main线程取得最终的结果值看是多少.
        while (Thread.activeCount() > 2){ // 2代表一个main线程和GC线程
            Thread.yield();
        }
        System.out.println(Thread.currentThread().getName()+"\t finally number value: "+myData.number);
    }
}

```

MyData`中的number被`volatile`修饰时的运行结果: 

```java
// 结果小于20000
main	 finally number value: 19940
```

`volatile`并**不能保证操作的原子性**。这是因为，比如一条number++的操作，会形成3条指令

```java
getfield    //读
iconst_1	//++常量1
iadd		//加操作
putfield	//写操作
```

假设有3个线程，分别执行number++，都先从主内存中拿到最开始的值，number=0，然后三个线程分别进行操作。假设线程0执行完毕，number=1，也立刻通知到了其它线程，但是此时线程1、2已经拿到了number=0，所以结果就是写覆盖，线程1、2将number变成1。

解决的方法: 

1. 对`addPlusPlus()`方法加锁。
2. 使用`java.util.concurrent.AtomicInteger`类。

使用`AtomicInteger`:

```java
class MyData{

    volatile int number = 0;
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
}
```

运行结果: volatile不保证原子性,出现了写覆盖的问题, 而AtomicInteger保证了原子性.

```java
main	 int type, finally number value: 18988
main	 AtomicInteger type, finally number value: 20000
```

#### 2.3 有序性

volatile可以保证**有序性**，也就是防止**指令重排序**。所谓指令重排序，就是出于优化考虑，CPU执行指令的顺序跟程序员自己编写的顺序不一致。就好比一份试卷，题号是老师规定的，是程序员规定的，但是考生（CPU）可以先做选择，也可以先做填空。

```
int x = 11; //语句1
int y = 12; //语句2
x = x + 5;  //语句3
y = x * x;  //语句4
```

以上例子，可能出现的执行顺序有1234、2134、1342，这三个都没有问题，最终结果都是x = 16，y=256。但是如果是4开头，就有问题了，y=0。这个时候就**不需要**指令重排序。

```java
public class ResortSeqDemo {
    int a=0;
    boolean flag=false;

    /**
     * 多线程下flag=true可能先执行，还没走到a=1就被挂起。
     *  其它线程进入method02的判断，修改a的值=5，而不是6。
     */
    public void method01(){
        a=1;
        flag=true;
    }
    public void method02(){
        if (flag){
            a+=5;
            System.out.println("*****retValue: "+a);
        }
    }
}
```

`volatile`底层是用CPU的**内存屏障**（Memory Barrier）指令来实现的，有两个作用，一个是保证特定操作的顺序性，二是保证变量的可见性。在指令之间插入一条Memory Barrier指令，告诉编译器和CPU，在Memory Barrier指令之间的指令不能被重排序。

### 3. 你在哪些地方用到过volatile?

> 单例模式的安全问题: 
>
> 常见的DCL（Double Check Lock）模式虽然加了同步，但是在多线程下依然会有线程安全问题。

```java
public class SingletonDemo {

    private static SingletonDemo instance = null;

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

```

原因是有指令重排的存在. 某一个线程在执行到第一次检测,读取到的instance不为null时,instance的引用对象**可能没有完成初始化**.`instance=new SingletonDem()`; 可以分为以下步骤(伪代码):

```java
memory=allocate();//1.分配对象内存空间
instance(memory);//2.初始化对象
instance=memory;//3.设置instance的指向刚分配的内存地址,此时instance!=null 
```

其中2、3没有数据依赖关系，**可能发生重排**。如果发生，此时内存已经分配，那么`instance=memory`不为null。如果此时线程挂起，`instance(memory)`还未执行，对象还未初始化。由于`instance!=null`，所以两次判断都跳过，最后返回的`instance`没有任何内容，还没初始化。

解决的方法就是对`singletondemo`对象添加上`volatile`关键字，禁止指令重排。















