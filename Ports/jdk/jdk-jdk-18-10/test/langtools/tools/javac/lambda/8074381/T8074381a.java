/*
 * @test /nodynamiccopyright/
 * @bug 8074381
 * @summary java.lang.AssertionError during compiling
 * @compile/fail/ref=T8074381a.out -XDrawDiagnostics T8074381a.java
 */
class T8074381a {
    interface Sup<X> {
        boolean m(X x);
    }

    interface Sub<X> extends Sup<String> {
        boolean m(String s);
    }

    @SuppressWarnings({"deprecation", "removal"})
    void testRaw() {
        Sub s1 = c -> true;
        Sub s2 = Boolean::new;
        Sub s3 = new Sub() {
            @Override
            public boolean m(String o) { return true; }
        };
    }

    @SuppressWarnings({"deprecation", "removal"})
    void testNonRaw() {
        Sub<Integer> s1 = c -> true;
        Sub<Integer> s2 = Boolean::new;
        Sub<Integer> s3 = new Sub<Integer>() {
            @Override
            public boolean m(String o) { return true; }
        };
    }
}
