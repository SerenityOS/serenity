/*
 * @test /nodynamiccopyright/
 * @author mcimadamore
 * @bug     6467183
 * @summary unchecked warning on cast of parameterized generic subclass
 * @compile/ref=T6467183b.out -XDrawDiagnostics -Xlint:unchecked T6467183b.java
 */

class T6665356b<T> {

    class A<S> {}
    class B<X> extends A<X> {}

    void cast(A<? extends Number> a) {
        Object o = (B<? extends Integer>)a;
    }
}
