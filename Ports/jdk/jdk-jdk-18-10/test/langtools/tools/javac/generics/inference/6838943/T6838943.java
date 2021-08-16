/**
 * @test /nodynamiccopyright/
 * @bug 6838943
 * @summary inference: javac is not handling type-variable substitution properly
 * @compile/fail/ref=T6838943.out -XDrawDiagnostics T6838943.java
 */
class T6838943 {
    static class A<X> {}
    static class B {}
    static class C<X> {
        <Z> void m(X x, Z z) {
            C<A<Z>> c = new C<A<Z>>();
            c.m(new A<B>(), new B()); //should fail
        }
    }
}
