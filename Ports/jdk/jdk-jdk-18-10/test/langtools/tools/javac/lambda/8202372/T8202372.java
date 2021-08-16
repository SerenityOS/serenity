/*
 * @test /nodynamiccopyright/
 * @bug 8202372
 * @summary Diagnostic with incorrect line info generated when compiling lambda expression
 * @compile/fail/ref=T8202372.out -XDrawDiagnostics T8202372.java
 */
class T8202372 {

    interface NonVoidFunc {
        String m();
    }

    interface VoidFunc {
        void m();
    }

    interface ParamFunc {
        void m(String s);
    }

    public void addVoid(VoidFunc v) {}
    public void addNonVoid(NonVoidFunc nv) {}
    public void addParam(ParamFunc p) {}

    void testVoid(T8202372 test) {
        test.addVoid(() -> "");
        test.addVoid(() -> { return ""; });
        test.addVoid(() -> { });
        test.addVoid(() -> { return; });
    }

    void testNonVoid(T8202372 test) {
        test.addNonVoid(() -> "");
        test.addNonVoid(() -> { return ""; });
        test.addNonVoid(() -> { });
        test.addNonVoid(() -> { return; });
    }

    void testParam(T8202372 test) {
        test.addParam(() -> {});
        test.addParam((String x) -> { });
        test.addParam((String x1, String x2) -> { });
        test.addParam((int x) -> { });
    }
}