/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  On-demand symbol completion during speculative attribution round fails to report error messages
 * @compile/fail/ref=Main.out -XDrawDiagnostics Main.java
 */
class Main {
    void test() {
        m(new A(new Object()));
        m(new A(null));
    }

    void m(Object o) {}
}
