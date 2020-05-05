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

> + 循环时间长开销很大
> + 只能保证一个共享变量的原子性, 多个变量依然要加锁
> + 引出来**ABA问题**

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



















