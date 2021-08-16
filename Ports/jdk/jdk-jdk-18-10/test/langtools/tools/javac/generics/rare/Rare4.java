/*
 * @test /nodynamiccopyright/
 * @bug 5024308
 * @summary "rare" types
 * @author gafter
 *
 * @compile/fail/ref=Rare4.out -XDrawDiagnostics  Rare4.java
 */

package rare4;

class A<T> {
    class B<U> {
    }
    static class C<V> {
    }
}

class Main {
    A<String>.C<String> ac1;
}
