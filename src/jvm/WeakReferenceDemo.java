package jvm;

import java.lang.ref.WeakReference;

/**
 * 若引用
 * @author zhanghao
 * @date 2020/5/8 - 13:55
 */
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
