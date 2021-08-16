/*
 * @test /nodynamiccopyright/
 * @bug 5024308
 * @summary "rare" types
 * @author gafter
 *
 * @compile/fail/ref=Rare3.out -XDrawDiagnostics  Rare3.java
 */

package rare3;

class A<T> {
    class B<U> {
    }
    static class C<V> {
    }
}

class Main {
    A.B<String> ab2;
}
