# JUC多线程及并发包

## 1. 谈谈你对volatile的理解

### 1.1 volatile是Java虚拟机提供的轻量级的同步机制

- 保证可见性
- 不保证原子性
- 禁止指令重排

### 1.2. JMM你谈谈

JMM(Java内存模型Java Memory Model,简称JMM)本身是一种抽象的概念 并不真实存在,它描述的是一组规则或规范通过规范定制了程序中各个变量(包括实例字段,静态字段和构成数组对象的元素)的访问方式.

JMM关于同步规定:

- 线程解锁前,必须把共享变量的值刷新回主内存
- 线程加锁前,必须读取主内存的最新值到自己的工作内存
- 加锁解锁是同一把锁

由于JVM运行程序的实体是线程,而每个线程创建时JVM都会为其创建一个工作内存(有些地方成为栈空间),工作内存是每个线程的私有数据区域,而Java内存模型中规定所有变量都存储在主内存,主内存是共享内存区域,所有线程都可访问,但线程对变量的操作(读取赋值等)必须在工作内存中进行,首先要将变量从主内存拷贝到自己的工作空间,然后对变量进行操作,操作完成再将变量写回主内存,不能直接操作主内存中的变量,各个线程中的工作内存储存着主内存中的变量副本拷贝,因此不同的线程无法访问对方的工作内存,线程间的通讯(传值) 必须通过主内存来完成,其简要访问过程如下图:

![JMM](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/JMM.jpg)

#### 1.2.1 可见性

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

#### 1.2.2 原子性

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

#### 1.2.3 有序性

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

单例模式的安全问题: 

常见的DCL（Double Check Lock）模式虽然加了同步，但是在多线程下依然会有线程安全问题。

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

## 2. CAS你知道吗

CAS的全称为**Compare-And-Swap**，它是一条CPU并发原语。它的功能是判断主内存某个位置的值是否为预期值，如果是则更改为新的值, 否则一直重试，直到一致为止。

```java
		AtomicInteger atomicInteger = new AtomicInteger(5);
        System.out.println(atomicInteger.compareAndSet(5, 2020) + "\t current: "+atomicInteger.get());
        System.out.println(atomicInteger.compareAndSet(5, 2020) + "\t current: "+atomicInteger.get());
```

运行结果: 

```java
true	 current: 2020
false	 current: 2020
```

第一次修改，期望值为5，主内存也为5，修改成功，为2020。第二次修改，期望值为5，主内存为2020，修改失败。

查看`AtomicInteger.getAndIncrement()`方法，发现其没有加`synchronized`**也实现了同步**。这是为什么？

### 2.1 CAS底层原理

`AtomicInteger`内部维护了`volatile int value`和`private static final Unsafe unsafe`两个比较重要的参数。

```java
public final int getAndIncrement() {
    return unsafe.getAndAddInt(this, valueOffset, 1);
}
```

`AtomicInteger.getAndIncrement()`调用了`Unsafe.getAndAddInt()`方法。`Unsafe`类的大部分方法都是`native`的，用来像C语言一样从底层操作内存。

```java
public final int getAndAddInt(Object var1, long var2, int var4) {
    int var5;
    do {
        var5 = this.getIntVolatile(var1, var2);
    } while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4));

    return var5;
}
```

假设线程A和线程B两个线程同时执行getAndAddInt操作(分别在不同的CPU上):

1.`AtomicInteger`里面的`value`原始值为3,即主内存中`AtomicInteger`的`value`为3,根据JMM模型,线程A和线程B各自持有一份值为3的`value`的副本分别到各自的工作内存.

2.线程A通过`getIntVolatile(var1,var2) `拿到value值3,这是线程A被挂起.

3.线程B也通过`getIntVolatile(var1,var2) `拿到value值3,此时刚好线程B没有被挂起并执行`compareAndSwapInt`方法比较内存中的值也是3 成功修改内存的值为4 线程B打完收工 一切OK.

 4.这是线程A恢复,执行`compareAndSwapInt`方法比较,发现自己手里的数值和内存中的数字4不一致,说明该值已经被其他线程抢先一步修改了,那A线程修改失败,只能重新来一遍了.

 5.线程A重新获取`value`值,因为变量`value`是`volatile`修饰,所以其他线程对他的修改,线程A总是能够看到,线程A继续执行`compareAndSwapInt`方法进行比较替换,直到成功.

### 2.2 CAS缺点

- 循环时间长开销很大

- 只能保证一个共享变量的原子性, 多个变量依然要加锁

- 引出来**ABA问题**

## 3. 原子类AtomicInteger的ABA问题

所谓ABA问题，就是比较并交换的循环，存在一个**时间差**，而这个时间差可能带来意想不到的问题。比如线程T1将值从A改为B，然后又从B改为A。线程T2看到的就是A，但是**却不知道这个A发生了更改**。尽管线程T2 CAS操作成功，但不代表就没有问题。 有的需求，比如CAS，**只注重头和尾**，只要首尾一致就接受。但是有的需求，还看重过程，中间不能发生任何修改，这就引出了`AtomicReference`原子引用。

### 3.1 AtomicReference

`AtomicInteger`对整数进行原子操作，如果是一个POJO呢？可以用`AtomicReference`来包装这个POJO，使其操作原子化。

```java
		User zs = new User("z3",22);
        User li4 = new User("li4",25);

        AtomicReference<User> userAtomicReference = new AtomicReference<>();
        userAtomicReference.set(zs);

        System.out.println(userAtomicReference.compareAndSet(zs,li4) + "\t" + userAtomicReference.get().toString());
        System.out.println(userAtomicReference.compareAndSet(zs,li4) + "\t" + userAtomicReference.get().toString());

```

运行结果: 

```java
true	User{name='li4', age=25}
false	User{name='li4', age=25}
```

### 3.2 ABA问题的解决

使用`AtomicStampedReference`类可以解决ABA问题。这个类维护了一个“**版本号**”Stamp，在进行CAS操作的时候，不仅要比较当前值，还要比较**版本号**。只有两者都相等，才执行更新操作。

```java
AtomicStampedReference.compareAndSet(expectedReference,newReference,oldStamp,newStamp);
```

详见[ABADemo](https://github.com/jackhusky/JUC-JVM-GC/blob/master/src/juc/ABADemo.java)

## 4. 集合类不安全问题

### 4.1 List

`ArrayList`不是线程安全类，在多线程同时写的情况下，会抛出`java.util.ConcurrentModificationException`异常。

```java
public class ContainerNotSafeDemo {

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < 30; i++){
            new Thread(() -> {
                list.add(UUID.randomUUID().toString().substring(0,8));
                System.out.println(list);
            },String.valueOf(i)).start();
        }
    }
}
```

解决方法: 

- 使用`Vector`（`ArrayList`所有方法加`synchronized`，太重）。
- 使用`Collections.synchronizedList()`转换成线程安全类。
- 使用`java.concurrent.CopyOnWriteArrayList`（推荐）。

#### 4.1.1 CopyOnWriteArrayList

这是JUC的类，通过**写时复制**来实现**读写分离**。比如其`add()`方法，就是先**复制**一个新数组，长度为原数组长度+1，然后将新数组最后一个元素设为添加的元素。

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        // 获取数组
        Object[] elements = getArray();
        int len = elements.length;
        // 复制数组得到新数组
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        // 把要添加的元素设置到新数组最后一个位置
        newElements[len] = e;
        // 设置新数组
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```

### 4.2 Set

跟List类似，`HashSet`和`TreeSet`都不是线程安全的，与之对应的有`CopyOnWriteSet`这个线程安全类。这个类底层维护了一个`CopyOnWriteArrayList`数组。

```java
private final CopyOnWriteArrayList<E> al;
public CopyOnWriteArraySet() {
    al = new CopyOnWriteArrayList<E>();
}
```

#### 4.2.1 HashSet和HashMap

`HashSet`底层是用`HashMap`实现的。既然是用`HashMap`实现的，那`HashMap.put()`需要传**两个参数**，而`HashSet.add()`只**传一个参数**，这是为什么？实际上`HashSet.add()`就是调用的`HashMap.put()`，只不过**Value**被写死了，是一个`private static final Object`对象。

### 4.3 Map

`HashMap`不是线程安全的，`Hashtable`是线程安全的，但是跟`Vector`类似，太重量级。所以也有类似CopyOnWriteMap，只不过叫`ConcurrentHashMap`。

关于集合不安全类请看[ContainerNotSafeDemo](https://github.com/jackhusky/JUC-JVM-GC/blob/master/src/juc/ContainerNotSafeDemo.java)

## 5. JAVA锁

### 5.1 公平锁和非公平锁

#### 5.1.1 是什么?

公平锁是指多个线程**按照申请锁的顺序**来获取锁类似排队打饭, 先来后到.

非公平锁是指在多线程获取锁的顺序**并不是按照申请锁的顺序**, 有可能后申请的线程比先申请的线程优先获取到锁,在高并发的情况下, 有可能造成**优先级反转**或者**饥饿现象**.

#### 5.1.2 区别?

公平锁在获取锁时先查看此锁维护的**等待队列**，**为空**或者当前线程是等待队列的**队首**，则直接占有锁，否则插入到等待队列，FIFO原则(先进先出)。非公平锁比较粗鲁，上来直接**先尝试占有锁**，失败则采用公平锁方式。非公平锁的优点是**吞吐量**比公平锁更大。

> `synchronized`和`java.util.concurrent.locks.ReentrantLock`默认都是**非公平锁**。`ReentrantLock`在构造的时候传入`true`则是**公平锁**。

### 5.2 可重入锁(递归锁)

可重入锁又叫递归锁，指的同一个线程在**外层方法**获得锁时，进入**内层方法**会自动获取锁。也就是说，线程可以进入任何一个它已经拥有锁的代码块。比如`get`方法里面有`set`方法，两个方法都有同一把锁，得到了`get`的锁，就自动得到了`set`的锁。

**可重入锁最大的作用就是避免死锁**

####锁的配对

锁之间要配对，加了几把锁，最后就得解开几把锁，下面的代码编译和运行都没有任何问题。但锁的数量不匹配会导致死循环。

```java
lock.lock();
lock.lock();
try{
    someAction();
}finally{
    lock.unlock();
}
```

详见[ReentrantLockDemo](https://github.com/jackhusky/JUC-JVM-GC/blob/master/src/juc/ReentrantLockDemo.java)

### 5.3 自旋锁

所谓自旋锁，就是尝试获取锁的线程不会**立即阻塞**，而是采用**循环的方式去尝试获取**。自己在那儿一直循环获取，就像“**自旋**”一样。这样的好处是减少**线程切换的上下文开销**，缺点是会**消耗CPU**。CAS底层的`getAndAddInt`就是**自旋锁**思想。

```java
//跟CAS类似，一直循环比较。
while (!atomicReference.compareAndSet(null, thread)) { }
```

详见[SpinLockDemo](https://github.com/jackhusky/JUC-JVM-GC/blob/master/src/juc/SpinLockDemo.java)

### 5.4 独占锁(写)/共享锁(读)/互斥锁

**读锁**是**共享的**，**写锁**是**独占的**。`java.util.concurrent.locks.ReentrantLock`和`synchronized`都是**独占锁**，独占锁就是**一个锁**只能被**一个线程**所持有。有的时候，需要**读写分离**，那么就要引入读写锁，即`java.util.concurrent.locks.ReentrantReadWriteLock`。

详见[ReadWriteLockDemo](https://github.com/jackhusky/JUC-JVM-GC/blob/master/src/juc/ReadWriteLockDemo.java)

## 6. CountDownLatch/CyclicBarrier/Semaphore使用过吗?

###6.1 CountDownLatch

`CountDownLatch`内部维护了一个**计数器**，只有当**计数器==0**时，某些线程才会停止阻塞，开始执行。

`CountDownLatch`主要有两个方法，`countDown()`来让计数器-1，`await()`来让线程阻塞。当`count==0`时，阻塞线程自动唤醒。

**案例一班长关门**：main线程是班长，6个线程是学生。只有6个线程运行完毕，都离开教室后，main线程班长才会关教室门。

**案例二秦灭六国**：只有6国都被灭亡后（执行完毕），main线程才会显示“秦国一统天下”。

**枚举类的使用**

在**案例二**中会使用到枚举类，因为灭六国，循环6次，想根据`i`的值来确定输出什么国，比如1代表楚国，2代表赵国。如果用判断则十分繁杂，而枚举类可以简化操作。

枚举类就像一个**简化的数据库**，枚举类名就像数据库名，枚举的项目就像数据表，枚举的属性就像表的字段。

关于`CountDownLatch`和枚举类的使用，请看 [CountDownLatchDemo](https://github.com/jackhusky/JUC-JVM-GC/tree/master/src/juc/CountDownLatchDemo.java)

### 6.2 CyclicBarrier

`CountDownLatch`是减，而`CyclicBarrier`是加，理解了`CountDownLatch`，`CyclicBarrier`就很容易。比如召集7颗龙珠才能召唤神龙，详见[CyclicBarrierDemo](https://github.com/jackhusky/JUC-JVM-GC/tree/master/src/juc/CyclicBarrierDemo.java)。

### 6.3 Semaphore

`CountDownLatch`的问题是**不能复用**。比如`count=3`，那么加到3，就不能继续操作了。而`Semaphore`可以解决这个问题，比如6辆车3个停车位，对于`CountDownLatch`**只能停3辆车**，而`Semaphore`可以停6辆车，车位空出来后，其它车可以占有，这就涉及到了`Semaphore.accquire()`和`Semaphore.release()`方法。

```java
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

运行结果: 
0	 抢到车位
1	 抢到车位
2	 抢到车位
0	 三秒后离开车位
3	 抢到车位
2	 三秒后离开车位
1	 三秒后离开车位
4	 抢到车位
5	 抢到车位
3	 三秒后离开车位
5	 三秒后离开车位
4	 三秒后离开车位
```

## 7. 阻塞队列

**概念: ** 当阻塞队列为空时，获取（take）操作是阻塞的；当阻塞队列为满时，添加（put）操作是阻塞的。

**好处**：阻塞队列不用手动控制什么时候该被阻塞，什么时候该被唤醒，简化了操作。

**体系**：`Collection`→`Queue`→`BlockingQueue`→七个阻塞队列实现类。

| 类名                    | 作用                             |
| ----------------------- | -------------------------------- |
| **ArrayBlockingQueue**  | 由**数组**构成的**有界**阻塞队列 |
| **LinkedBlockingQueue** | 由**链表**构成的**有界**阻塞队列 |
| PriorityBlockingQueue   | 支持优先级排序的无界阻塞队列     |
| DelayQueue              | 支持优先级的延迟无界阻塞队列     |
| **SynchronousQueue**    | 单个元素的阻塞队列               |
| LinkedTransferQueue     | 由链表构成的无界阻塞队列         |
| LinkedBlockingDeque     | 由链表构成的双向阻塞队列         |

粗体标记的三个用得比较多，许多消息中间件底层就是用它们实现的。

需要注意的是`LinkedBlockingQueue`虽然是有界的，但有个巨坑，其默认大小是`Integer.MAX_VALUE`，高达21亿，一般情况下内存早爆了（在线程池的`ThreadPoolExecutor`有体现）。

**API**：抛出异常是指当队列满时，再次插入会抛出异常；返回布尔是指当队列满时，再次插入会返回false；阻塞是指当队列满时，再次插入会被阻塞，直到队列取出一个元素，才能插入。超时是指当一个时限过后，才会插入或者取出。API使用见[BlockingQueueDemo](https://github.com/jackhusky/JUC-JVM-GC/tree/master/src/juc/BlockingQueueDemo.java)。

| 方法类型 | 抛出异常  | 返回布尔   | 阻塞     | 超时                     |
| -------- | --------- | ---------- | -------- | ------------------------ |
| 插入     | add(E e)  | offer(E e) | put(E e) | offer(E e,Time,TimeUnit) |
| 取出     | remove()  | poll()     | take()   | poll(Time,TimeUnit)      |
| 队首     | element() | peek()     | 无       | 无                       |

### SynchronousQueue

`SynchronousQueue`没有容量, 与其他`BlcokingQueue`不同,`SynchronousQueue`是一个不存储元素的`BlcokingQueue`, 每个`put`操作必须要等待一个`take`操作, 否则不能继续添加元素, 反之亦然.

详见 [SynchronousQueueDemo](https://github.com/jackhusky/JUC-JVM-GC/tree/master/src/juc/SynchronousQueueDemo.java)

### 阻塞队列的应用——生产者消费者

#### 传统模式

传统模式使用`Lock`来进行操作，需要手动加锁、解锁。详见[ProdConsumer_TraditionDemo](https://github.com/jackhusky/JUC-JVM-GC/tree/master/src/juc/ProdConsumer_TraditionDemo.java)。

```java
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
```



`synchronized`和`Lock`有什么区别?

- `synchronized`属于JVM层面是关键字, `ReentrantLock`属于API层面是具体的类
- `synchronized` 不需要手动释放锁, 当`synchronized` 代码执行完后系统会自动让线程释放对锁的占用, `ReentrantLock`需要手动释放锁, 如果没有释放锁, 就有可能出现死锁现象.
- `synchronized` 不可中断, 除非抛出异常或者正常运行完成, `ReentrantLock`可中断, 设置超时方法或者调用interrupt()方法可中断
- `synchronized` 非公平锁, `ReentrantLock`两者都可以,默认非公平锁.
- 锁绑定多个条件`Condition`: `synchronized` 没有,`ReentrantLock`用来实现分组唤醒需要唤醒的线程们, 可以精确唤醒, 而不是像`synchronized` 要么随机唤醒一个线程要么唤醒全部线程. 详见[SyncAndReentrantLockDemo](https://github.com/jackhusky/JUC-JVM-GC/tree/master/src/juc/SyncAndReentrantLockDemo.java)

####阻塞队列模式

使用阻塞队列就不需要手动加锁了，详见[ProdConsumerBlockQueueDemo](https://github.com/jackhusky/JUC-JVM-GC/tree/master/src/juc/ProdConsumerBlockQueueDemo.java)

```java
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
```

## 8. Callable接口

**与Runnable的区别**：

1. Callable带返回值。
2. 会抛出异常。
3. 覆写`call()`方法，而不是`run()`方法。

```java
public class CallableDemo {
    //实现Callable接口
    class MyThread implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("callable come in ...");
            return 1024;
        }
    }
    
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //创建FutureTask类，接受MyThread。    
        FutureTask<Integer> futureTask = new FutureTask<>(new MyThread());
        //将FutureTask对象放到Thread类的构造器里面。
        new Thread(futureTask, "AA").start();
        int result01 = 100;
        //用FutureTask的get方法得到返回值。
        int result02 = futureTask.get();
        System.out.println("result=" + (result01 + result02));
    }
}
```

## 9. 阻塞队列的应用——线程池

**概念**：线程池主要是控制运行线程的数量，将待处理任务放到等待队列，然后创建线程执行这些任务。如果超过了最大线程数，则等待。

**优点**：

1. 线程复用：不用一直new新线程，重复利用已经创建的线程来降低线程的创建和销毁开销，节省系统资源。
2. 提高响应速度：当任务达到时，不用创建新的线程，直接利用线程池的线程。
3. 管理线程：可以控制最大并发数，控制线程的创建等。

**体系**：`Executor`→`ExecutorService`→`AbstractExecutorService`→`ThreadPoolExecutor`。`ThreadPoolExecutor`是线程池创建的核心类。类似`Arrays`、`Collections`工具类，`Executor`也有自己的工具类`Executors`。

### 9.1 常用线程池编码实现

`Executors.newFixedThreadPool()` : 使用`LinkedBlockingQueue`实现，定长线程池。

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>());
}
```

`Executors.newSingleThreadExecutor()` : 使用`LinkedBlockingQueue`实现，一池只有一个线程。

```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>()));
}
```

`Executors.newCachedThreadPool() `: 使用`SynchronousQueue`实现，变长线程池。

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
}
```

### 9.2 线程池创建的七个参数

| 参数            | 意义                       |
| --------------- | -------------------------- |
| corePoolSize    | 线程池常驻核心线程数       |
| maximumPoolSize | 能够容纳的最大线程数       |
| keepAliveTime   | 空闲线程存活时间           |
| unit            | 存活时间单位               |
| workQueue       | 存放提交但未执行任务的队列 |
| threadFactory   | 创建线程的工厂类           |
| handler         | 等待队列满后的拒绝策略     |

**理解**：线程池 --> **银行网点**。

`corePoolSize`就像银行的“**当值窗口**“，比如今天有**2位柜员**在受理**客户请求**（任务）。如果超过2个客户，那么新的客户就会在**等候区**（等待队列`workQueue`）等待。当**等候区**也满了，这个时候就要开启“**加班窗口**”，让其它3位柜员来加班，此时达到**最大窗口**`maximumPoolSize`，为5个。如果开启了所有窗口，等候区依然满员，此时就应该启动”**拒绝策略**“`handler`，告诉不断涌入的客户，叫他们不要进入，已经爆满了。由于不再涌入新客户，办完事的客户增多，窗口开始空闲，这个时候就通过`keepAlivetTime`将多余的3个”加班窗口“取消，恢复到2个”当值窗口“。

### 9.3 线程池底层原理

![线程池原理1](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/线程池底层原理1.png)

![线程池原理2](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/线程池底层原理2.jpg)

- 在创建了线程池后，等待提交过来的任务请求
- 当调用`execute() `方法添加一个请求任务时，线程池会做如下判断：
  - 如果正在运行的线程数量小于`corePoolSize`，那么马上创建线程运行这个任务；
  - 如果正在运行的线程数量大于或等于`corePoolSize`，那么将这个任务放入队列；
  - 如果这时候队列满了且正在运行的线程数量还小于`maximumPoolSize`，那么还是要创建非核心线程立刻运行这个任务；
  - 如果队列满了且正在运行的线程数量大于或等于`maximumPoolSize`，那么线程池会启动饱和拒绝策略来执行。
- 当一个线程完成任务时，他会从队列中取下一个任务来执行。
- 当一个线程无事可做超过一定的时间(`keepAliveTime`)时，线程池会判断：
  - 如果当前运行的线程数大于`corePoolSize`，那么这个线程就被停掉。
  - 所以线程池的所有任务完成后它最终会收缩到`corePoolSize`的大小。

### 9.4 线程池的拒绝策略

当等待队列满时，且达到最大线程数，再有新任务到来，就需要启动拒绝策略。JDK提供了四种拒绝策略，分别是：

1. **AbortPolicy**：默认的策略，直接抛出`RejectedExecutionException`异常，阻止系统正常运行。
2. **CallerRunsPolicy**：既不会抛出异常，也不会终止任务，而是将任务返回给调用者。
3. **DiscardOldestPolicy**：抛弃队列中等待最久的任务，然后把当前任务加入队列中尝试再次提交任务。
4. **DiscardPolicy**：直接丢弃任务，不做任何处理。

### 9.5 实际生产使用哪一个线程池？

`newFixedThreadPool()，newSingleThreadExecutor()，newCachedThreadPool()`都不用，原因就是`FixedThreadPool`和`SingleThreadExecutor`底层都是用`LinkedBlockingQueue`实现的，这个队列最大长度为`Integer.MAX_VALUE`，显然会导致OOM。所以实际生产一般自己通过`ThreadPoolExecutor`的7个参数，自定义线程池。

```java
	ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2,
                5,
                1L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(3),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.DiscardPolicy());
```

**自定义线程池参数选择**：对于CPU密集型任务，最大线程数是CPU线程数+1。对于IO密集型任务，尽量多配点，可以是CPU线程数*2，或者CPU线程数/(1-阻塞系数)。

> Systemout.println(Runtime.getRuntime().availableProcessors()); 查看CPU核数.
>
> CPU密集型: 该任务需要大量的运算，而没有阻塞，CPU全速运行。一般公式：CPU核数+1一个线程的线程池
>
> IO密集型：该任务需要大量的IO，即大量的阻塞，故需要多配置线程数。在IO密集型任务中使用多线程可以大大的加速程序运行，即使在单核CPU上，这种加速主要就是利用了被浪费掉的阻塞时间。参考公式：CPU核数/1-阻塞系数（阻塞系数在0.8~0.9之间）比如8核CPU：8/1-0.9=80个线程数

## 10. 死锁编码和定位

```java
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
```

主要是两个命令配合起来使用，定位死锁。

**jps**指令：`jps -l`可以查看运行的Java进程。

```shell
1244 org.jetbrains.jps.cmdline.Launcher
14492 juc.DeadLockDemo
```

**jstack**指令：`jstack pid`可以查看某个Java进程的堆栈信息，同时分析出死锁。

```shell
===================================================
"BBB":
        at juc.HoldLockThread.run(DeadLockDemo.java:22)
        - waiting to lock <0x000000076b57fb10> (a java.lang.String)
        - locked <0x000000076b57fb48> (a java.lang.String)
        at java.lang.Thread.run(Thread.java:748)
"AAA":
        at juc.HoldLockThread.run(DeadLockDemo.java:22)
        - waiting to lock <0x000000076b57fb48> (a java.lang.String)
        - locked <0x000000076b57fb10> (a java.lang.String)
        at java.lang.Thread.run(Thread.java:748)

Found 1 deadlock.
```

