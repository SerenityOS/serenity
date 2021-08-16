/*
 * @test /nodynamiccopyright/
 * @bug 5024308
 * @summary "rare" types
 * @author gafter
 *
 * @compile/fail/ref=Rare5.out -XDrawDiagnostics  Rare5.java
 */

package rare5;

class A<T> {
    class B<U> {
    }
    static class C<V> {
    }
}

class Main {
    A<String>.C ac2;
}
