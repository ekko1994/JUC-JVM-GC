package jvm;

import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * @author zhanghao
 * @date 2020/5/8 - 14:18
 */
public class WeakHashMapDemo {
    public static void main(String[] args) {
        myHashMap();
        System.out.println("===============");
        myWeakHashMap();
    }

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
}
