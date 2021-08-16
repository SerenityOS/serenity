/*
 * @test /nodynamiccopyright/
 * @bug 6910550
 *
 * @summary javac 1.5.0_17 fails with incorrect error message
 * @compile/fail/ref=T6910550a.out -XDrawDiagnostics T6910550a.java
 *
 */
import java.util.*;

class T6910550a {
    void m(List<String> ls) {}
    void m(List<Integer> li) {}

    { m(Arrays.asList(12)); }
}
