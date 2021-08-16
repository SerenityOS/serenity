/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that inner scopes are left after a lambda check exception has been thrown
 * @compile/fail/ref=LambdaExpr19.out -XDrawDiagnostics LambdaExpr19.java
 */
class LambdaExpr19 {

    interface SAM {
        String m();
    }

    void m(SAM s) { }

    void testTry() {
        m(() -> {
                try { return 1; }
                catch (Exception e) { }
            });
    }

    void testTryWithResources() {
        m(() -> {
                try (AutoCloseable c = null) { return 1; }
                catch (Exception e) { }
            });
    }

    void testSwitch() {
        m(() -> {
                switch (1) {
                    default: return 1;
                }
            });
    }

    void testFor() {
        m(() -> {
                for (;;) {
                    return 1;
                }
            });
    }

    void testForeach() {
        m(() -> {
                for (Object o : new Object[] { null , null }) {
                    return 1;
                }
            });
    }
}
