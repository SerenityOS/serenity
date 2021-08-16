/*
 * @test /nodynamiccopyright/
 * @bug 5024308
 * @summary "rare" types
 * @author gafter
 *
 * @compile/fail/ref=Rare2.out -XDrawDiagnostics  Rare2.java
 */

package rare2;

class A<T> {
    class B<U> {
    }
    static class C<V> {
    }
}

class Main {
    A<String>.B ab1;
}
