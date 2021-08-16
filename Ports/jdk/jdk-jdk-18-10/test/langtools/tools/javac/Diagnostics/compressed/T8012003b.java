/**
 * @test /nodynamiccopyright/
 * @bug     8012003
 * @summary Method diagnostics resolution need to be simplified in some cases
 *          test lambda-related overload resolution simplifications
 * @compile/fail/ref=T8012003b.out -XDrawDiagnostics -Xdiags:compact T8012003b.java
 */

class T8012003b {

    interface Consumer_V<X> {
        void m(X x);
    }

    interface Consumer_NV<X> {
        Integer m(X x);
    }

    void m1(Runnable r) { }
    void m1(Runnable r, String s) { }

    void m2(Consumer_V<Integer> ci) { }

    void m3(Consumer_NV<String> ci) { }

    void g(String arg) { }
    String g2(String arg) { return arg; }

    void test() {
        m1(this::g);
        m1(()->1);
        m1(()->false ? "" : "");
        m2(this::g);
        m3(this::g2);
        m3(this::k);
    }
}
