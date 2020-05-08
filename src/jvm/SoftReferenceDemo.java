package jvm;

import java.lang.ref.SoftReference;

/**
 * 软引用
 * @author zhanghao
 * @date 2020/5/8 - 13:22
 */
public class SoftReferenceDemo {

    public static void main(String[] args) {
//        softRef_Memory_Enough();
        softRef_Memory_NotEnough();
    }

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

}
