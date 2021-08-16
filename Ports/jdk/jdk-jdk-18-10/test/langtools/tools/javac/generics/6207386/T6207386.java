/*
 * @test  /nodynamiccopyright/
 * @bug     6207386
 * @summary Undecidable type system leads to crash
 * @author  Martin Odersky
 * @compile/fail/ref=T6207386.out -XDrawDiagnostics T6207386.java
 */

public class T6207386 {
    static class F<T> {}
    static class C<X extends F<F<? super X>>> {
        C(X x) {
            F<? super X> f = x;
        }
    }
}
