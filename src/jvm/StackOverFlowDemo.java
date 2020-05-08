package jvm;

/**
 * @author zhanghao
 * @date 2020/5/8 - 15:48
 */
public class StackOverFlowDemo {

    public static void main(String[] args) {
        stackOverFlow();
    }

    private static void stackOverFlow() {
        stackOverFlow();
    }
}
