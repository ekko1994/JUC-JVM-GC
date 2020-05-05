package juc;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zhanghao
 * @date 2020/5/5 - 16:21
 */
class User{
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}

public class AtomicReferenceDemo {
    public static void main(String[] args) {
        User zs = new User("z3",22);
        User li4 = new User("li4",25);

        AtomicReference<User> userAtomicReference = new AtomicReference<>();
        userAtomicReference.set(zs);

        System.out.println(userAtomicReference.compareAndSet(zs,li4) + "\t" + userAtomicReference.get().toString());
        System.out.println(userAtomicReference.compareAndSet(zs,li4) + "\t" + userAtomicReference.get().toString());
    }
}
