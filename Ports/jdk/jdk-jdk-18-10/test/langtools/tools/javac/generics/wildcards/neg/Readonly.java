/*
 * @test /nodynamiccopyright/
 * @bug 4916607 4931647
 * @summary an extends-bound (covariant) wildcard is like readonly
 * @author gafter
 *
 * @compile/fail/ref=Readonly.out -XDrawDiagnostics Readonly.java
 */

class Err<T> {
    Err<T> get() { return null; }
    void put(Err<T> t) {}

    static void f(Err<? extends String> e) {
        e.put(e.get());
    }
}
