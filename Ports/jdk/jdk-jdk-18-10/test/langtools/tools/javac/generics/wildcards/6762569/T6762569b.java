/*
 * @test    /nodynamiccopyright/
 * @bug     6762569 8078024
 * @summary Javac crashes with AssertionError in Types.containedBy
 * @compile/fail/ref=T6762569b.out -XDrawDiagnostics  T6762569b.java
 */
import java.util.*;

class T6762569b {
    <T> void m(T t, List<? super List<T>> list) {}

    void test(List<? super List<? extends Number>> list) {
        m("", list);
    }
}
