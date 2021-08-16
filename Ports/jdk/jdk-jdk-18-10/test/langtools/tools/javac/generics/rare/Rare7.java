/*
 * @test /nodynamiccopyright/
 * @bug 5024308
 * @summary "rare" types
 * @author gafter
 *
 * @compile/fail/ref=Rare7.out -XDrawDiagnostics  Rare7.java
 */

package rare7;

class A<T> {
    class B<U> {
        T t;
    }

    static class C {
        {
            B<String> b = null;
        }
    }
}
