package jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数配置: -Xms10m -Xmx10m -XX:MaxDirectMemorySize=5m -XX:+PrintGCDetails
 * @author zhanghao
 * @date 2020/5/8 - 16:37
 */
public class GCOverheadDemo {
    public static void main(String[] args) {
        int i = 0;
        List<String> list = new ArrayList<>();
        try {
            while (true) {
                list.add(String.valueOf(++i).intern());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("************i" + i);
            throw e;
        }
    }
}
