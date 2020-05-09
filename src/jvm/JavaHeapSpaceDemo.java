package jvm;

import java.util.Random;

/**
 * @author zhanghao
 * @date 2020/5/8 - 16:18
 */
public class JavaHeapSpaceDemo {
    public static void main(String[] args) {
        String str = "aaa";
        while (true){
            str += str + new Random().nextInt(11111111) + new Random().nextInt(22222222);
            str.intern();
        }
    }
}
