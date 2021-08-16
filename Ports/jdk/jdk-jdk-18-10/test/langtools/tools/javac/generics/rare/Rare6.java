/*
 * @test /nodynamiccopyright/
 * @bug 5024308
 * @summary "rare" types
 * @author gafter
 *
 * @compile/fail/ref=Rare6.out -XDrawDiagnostics  Rare6.java
 */

package rare6;

class A<T> {
    class B<U> {
        T t;
    }

    static class C {
        {
            B b = null; // static error
            // b.t = "foo";
        }
    }
}
