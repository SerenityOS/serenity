/*
 * @test /nodynamiccopyright/
 * @bug 8203436
 * @summary javac should fail early when emitting illegal signature attributes
 * @compile/fail/ref=T8203436b.out -XDrawDiagnostics T8203436b.java
 */

class T8203436b<X> {
    interface A { }
    interface B { }

    class Inner { }

    <Z extends A & B> T8203436b<Z> m() { return null; }

    void test() {
        m().new Inner() { };
    }
}
