/*
 * @test /nodynamiccopyright/
 * @bug 8012685
 * @summary Spurious raw types warning when using unbound method references
 * @compile/fail/ref=MethodReference67.out -Werror -Xlint:rawtypes -XDrawDiagnostics MethodReference67.java
 */
import java.util.*;

class MethodReference67 {
    interface Foo<X> {
        void m(List<X> lx, X x);
    }

    void test() {
        Foo<String> fs1 = List::add; //no raw warnings here!
        Foo fs2 = List::add;
    }
}
