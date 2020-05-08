# JVM核心知识点

## 1. JVM内存结构

![JVM内存结构](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/JVM体系结构.bmp)

基本结构与之前类似，只是Java8取消了之前的“永久代”，取而代之的是“元空间”——**Metaspace**，两者本质是一样的。“永久代”使用的是JVM的堆内存，而“元空间”是直接使用的本机物理内存。

## 2. GC Roots

### 2.1 如果判断一个对象可以被回收？

- 引用计数算法: 维护一个计数器，如果有对该对象的引用，计数器+1，反之-1。无法解决循环引用的问题。

- 可达性分析算法: 所谓的“GC roots”或者说tracing GC 的“根集合”就是一组必须活跃的引用。

  > 这个算法的基本思路就是通过一系列的称为“GC Roots”的对象作为起始点，从这些节点开始向下搜索，搜索所走过的路径称为引用链，当一个对象到GC Roots没有任何引用链相连时，则证明此对象是不可用的。

  ![GC Roots](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/GC_Roots.bmp)

### 2.2 哪些对象可以作为GC Roots？

- 虚拟机栈（栈帧中的局部变量）中引用的对象。
- 方法区中类静态属性引用的对象。

- 方法区中常量引用的对象。

- 本地方法栈（native）中引用的对象。

## 3. 盘点查看JVM系统默认值

### 3.1 JVM的参数类型

#### 3.1.1 标配参数

比如`-version`、`-help`、`-showversion`等，几乎不会改变。

#### 3.1.2 X参数(了解)

比如`-Xint`解释执行模式；`-Xcomp`第一次使用就编译本地代码；`-Xmixed`，开启混合模式（默认）。

#### 3.1.3 XX参数

**重要，用于JVM调优。**

##### 3.1.3.1 布尔类型

**公式**：`-XX:+某个属性`、`-XX:-某个属性`，开启或关闭某个功能。比如`-XX:+PrintGCDetails`，开启GC详细信息。

##### 3.1.3.1 KV类型

**公式**：`-XX:属性key=值value`。比如`-XX:Metaspace=128m`、`-XX:MaxTenuringThreshold=15`。

> `-Xms`和`-Xmx`十分常见，用于设置**初始堆大小**和**最大堆大小**。第一眼看上去，既不像X参数，也不像XX参数。实际上`-Xms`等价于`-XX:InitialHeapSize`，`-Xmx`等价于`-XX:MaxHeapSize`。所以`-Xms`和`-Xmx`属于XX参数。

### 3.2 查看JVM参数

#### 3.2.1 查看当前运行程序某个参数的配置

使用`jps -l`配合`jinfo -flag JVM参数 pid` 。先用`jsp -l`查看java进程，选择某个进程号。

```shell
G:\IDEA-workspace\JUC&JVM&GC>jps -l
11764 org.jetbrains.kotlin.daemon.KotlinCompileDaemon
13908 sun.tools.jps.Jps
1508 org.jetbrains.jps.cmdline.Launcher
2724
5388 jvm.HelloGC
```

`jinfo -flag PrintGCDetails 5388`可以查看5388 Java进程的`PrintGCDetails`参数信息。

```shell
G:\IDEA-workspace\JUC&JVM&GC>jinfo -flag PrintGCDetails 5388
-XX:-PrintGCDetails
```

#### 3.2.2 查看当前运行程序所有参数的配置

使用`jps -l`配合`jinfo -flags pid`可以查看所有参数。

#### 3.2.3 查看JVM默认值

**查看初始默认值公式**: `java -XX:+PrintFlagsInitial -version` 或者 `java -XX:+PrintFlagsInitial`

```shell
G:\IDEA-workspace\JUC&JVM&GC>java -XX:+PrintFlagsInitial
[Global flags]
    uintx AdaptiveSizeDecrementScaleFactor          = 4                                   {product}
    uintx AdaptiveSizeMajorGCDecayTimeScale         = 10                                  {product}
    uintx AdaptiveSizePausePolicy                   = 0                                   {product}
......
     bool ZeroTLAB                                  = false                               {product}
     intx hashCode                                  = 5                                   {product}
```

**主要查看修改更新公式**: `java -XX:+PrintFlagsFinal ` 或者 `java -XX:+PrintFlagsFinal -version`

```shell
G:\IDEA-workspace\JUC&JVM&GC>java -XX:+PrintFlagsFinal -version
[Global flags]
    uintx AdaptiveSizeDecrementScaleFactor          = 4                                   {product}
    uintx AdaptiveSizeMajorGCDecayTimeScale         = 10                                  {product}
......
 bool PrintFlagsFinal                          := true                                {product}
......
```

> 注意: 带冒号的是JVM或者是人为修改过的更新值

**查看常见参数**: 如果不想查看所有参数，可以用`java -XX:+PrintCommandLineFlags -version`查看常用参数。

```shell
G:\IDEA-workspace\JUC&JVM&GC>java -XX:+PrintCommandLineFlags -version
-XX:InitialHeapSize=266553600 -XX:MaxHeapSize=4264857600 -XX:+PrintCommandLineFlags -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:-Use
LargePagesIndividualAllocation -XX:+UseParallelGC
java version "1.8.0_172"
Java(TM) SE Runtime Environment (build 1.8.0_172-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.172-b11, mixed mode)
```

## 4. JVM 常用参数

### 4.1 -Xmx/-Xms

-Xmx：最大分配内存，默认为物理内存1/4，等价于`-XX:MaxHeapSize`

-Xms：初始大小内存，默认为物理内存1/64，等价于`-XX:InitialHeapSize`

### 4.2 -Xss

设置单个线程栈的大小，一般默认为512K~1024K，等价于`-XX:ThreadStackSize`

> 根据操作系统的不同，有不同的值。比如64位的Linux系统是1024K，而Windows系统依赖于虚拟内存。

### 4.3 -Xmn

设置年轻代大小，一般不调

### 4.4 -XX:MetaspaceSize

设置元空间大小

### 4.5 -XX:+PrintGCDetails

输出`GC`收集信息，包含`GC`和`Full GC`信息。

![young gc](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/young_gc.bmp)

![full gc](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/full_gc.bmp)

### 4.6 -XX:SurvivorRatio

新生代中，`Eden`区和两个`Survivor`区的比例，默认是`8:1:1`。通过`-XX:SurvivorRatio=4`改成`4:1:1`

### 4.7 -XX:NewRatio

老生代和新年代的比列，默认是2，即老年代占2，新生代占1。如果改成`-XX:NewRatio=4`，则老年代占4，新生代占1。

 ### 4.8. -XX:MaxTenuringThreshold

新生代设置进入老年代的时间，默认是新生代逃过15次GC后，进入老年代。如果改成0，那么对象不会在新生代分配，直接进入老年代。

## 5. 强引用、软引用、弱引用、虚引用分别是什么?

### 5.1 强引用

使用`new`方法创造出来的对象，默认都是强引用。GC的时候，就算**内存不够**，抛出`OutOfMemoryError`也不会回收对象，**死了也不回收**。

```java
Object o1=new Object();
Object o2=new Object();
o1=null;
System.gc();
System.out.println(o2); //java.lang.Object@4554617c
```

### 5.2 软引用

需要用`Object.Reference.SoftReference`来显示创建。**如果内存够**，GC的时候**不回收**。**内存不够**，**则回收**。常用于内存敏感的应用，比如高速缓存。

```java
    /**
     * 内存够用的情况下保留
     */
    private static void softRef_Memory_Enough() {
        Object o1 = new Object();
        SoftReference<Object> softReference = new SoftReference<>(o1);

        o1 = null;
        System.gc();

        System.out.println(o1);
        System.out.println(softReference.get());
    } 
	// ********************************************************************** //
	运行结果: 
	null
	java.lang.Object@4554617c
```

```java
	/**
     *  故意产生大对象配置小内存,产生OOM,看软应用回收情况
     *  配置参数: -Xms5m -Xmx5m -XX:+PrintGCDetails
     */
    private static void softRef_Memory_NotEnough() {
        Object o1 = new Object();
        SoftReference<Object> softReference = new SoftReference<>(o1);
        System.out.println(o1);
        System.out.println(softReference.get());
        o1 = null;
        try {
            byte[] bytes = new byte[30*1024*1024];
        }catch (Exception exception){
            exception.printStackTrace();
        }finally {
            System.out.println(o1);
            System.out.println(softReference.get());
        }
    }
    // ********************************************************************** //
    运行结果: 
    [GC (Allocation Failure) [PSYoungGen: 1024K->504K(1536K)] 1024K->568K(5632K), 			0.0009868 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
    java.lang.Object@4554617c
    java.lang.Object@4554617c
    ......
    null
    null
    ......
```

### 5.3 弱引用

需要用`Object.Reference.WeakReference`来显示创建。**无论内存够不够，GC的时候都回收**，也可以用在高速缓存上。

```java
public class WeakReferenceDemo {

    public static void main(String[] args) {
        Object o = new Object();
        WeakReference<Object> weakReference = new WeakReference<>(o);
        System.out.println(o);
        System.out.println(weakReference.get());
        o = null;
        System.gc();
        System.out.println("*************************");
        System.out.println(o);
        System.out.println(weakReference.get());
    }
}
// ********************************************************************** //
运行结果: 
java.lang.Object@4554617c
java.lang.Object@4554617c
*************************
null
null
```

#### 5.3.1 WeakHashMap

传统的`HashMap`就算`key==null`了，`GC`也不会回收键值对。但是如果是`WeakHashMap`，一旦发生`GC`，且`key==null`时，会回收这个键值对。

```java
	private static void myHashMap() {
        HashMap<Integer, String> map = new HashMap<>();
        Integer key = new Integer(1);
        String value = "HashMap";
        map.put(key,value);
        key = null;
        System.out.println(map);
        System.gc();
        System.out.println(map + "---->" + map.size());
    }
    // ********************************************************************** //
    运行结果: 
	{1=HashMap}
	{1=HashMap}---->1
```

```java
	private static void myWeakHashMap() {
        WeakHashMap<Integer, String> map = new WeakHashMap<>();
        Integer key = new Integer(2);
        String value = "WeakHashMap";
        map.put(key,value);
        key = null;
        System.out.println(map);
        System.gc();
        System.out.println(map + "---->" + map.size());
    }
	// ********************************************************************** //
    运行结果:
	{2=WeakHashMap}
	{}---->0
```

#### 5.3.2 引用队列

弱引用、虚引用被回收后，会被放到引用队列里面，通过`poll`方法可以得到。关于引用队列和弱、虚引用的配合使用

### 5.4 虚引用

软应用和弱引用可以通过`get()`方法获得对象，但是虚引用的`get()`方法总是返回null。虚引形同虚设，在任何时候都可能被GC，不能单独使用，必须配合**引用队列（ReferenceQueue）来使用。设置虚引用的唯一目的**，就是在这个对象被回收时，收到一个**通知**以便进行后续操作，有点像`Spring`的后置通知。

```java
public class PhantomReferenceDemo {
    public static void main(String[] args) {
        Object o = new Object();
        ReferenceQueue referenceQueue = new ReferenceQueue();
        PhantomReference<Object> phantomReference = new PhantomReference<>(o,referenceQueue);

        System.out.println(o);
        System.out.println(phantomReference.get());
        System.out.println(referenceQueue.poll());

        System.out.println("**************************");
        o = null;
        System.gc();
        System.out.println(o);
        System.out.println(phantomReference.get());
        System.out.println(referenceQueue.poll());

    }
}
// ********************************************************************** //
运行结果:
null
null
**************************
null
null
java.lang.ref.PhantomReference@74a14482
```

### 5.5 小总结

![GC Roots 以及四大引用](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/GC_Root以及四大引用.bmp)

## 6. 请谈谈你对OOM的认识

`java.lang.StackOverflowError` 和 `java.lang.OutOfMemoryError` 都属于错误，不是异常。

![认识OOM](https://github.com/jackhusky/JUC-JVM-GC/blob/master/imgs/认识OOM.png)

### 6.1 java.lang.StackOverflowError

```java
public class StackOverFlowDemo {

    public static void main(String[] args) {
        stackOverFlow();
    }

    private static void stackOverFlow() {
        stackOverFlow();
    }
}
```

