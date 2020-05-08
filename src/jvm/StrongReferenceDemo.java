package jvm;

/**
 * 强引用
 * @author zhanghao
 * @date 2020/5/8 - 11:37
 */
public class StrongReferenceDemo {
    public static void main(String[] args) {
        Object o1=new Object();
        Object o2=new Object();
        o1=null;
        System.gc();
        System.out.println(o2);
    }
}
