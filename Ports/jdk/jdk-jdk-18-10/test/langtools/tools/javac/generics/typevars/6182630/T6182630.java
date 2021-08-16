/*
 * @test /nodynamiccopyright/
 * @bug 6182630
 * @summary Method with parameter bound to raw type avoids unchecked warning
 * @author Peter von der Ah\u00e9
 * @compile/ref=T6182630.out -XDrawDiagnostics -Xlint:unchecked T6182630.java
 */

public class T6182630 {
    static class Foo<X> {
        public X x;
        public void m(X x) { }
    }
    interface Bar {}
    <T extends Foo, S extends Foo & Bar> void test1(T t, S s) {
        t.x = "BAD";
        t.m("BAD");
        t.m(t.x);
        s.x = "BAD";
        s.m("BAD");
        s.m(s.x);
    }
}
