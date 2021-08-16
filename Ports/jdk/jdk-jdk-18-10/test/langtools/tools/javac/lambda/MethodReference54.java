/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  method call with bad qualifier generates NPE if argument is a method reference
 * @compile/fail/ref=MethodReference54.out -XDrawDiagnostics MethodReference54.java
 */
class MethodReference54 {

    interface SAM {
        void m();
    }

    void test() {
        nonExistent.m(MethodReference54::get);
    }

    static String get() { return ""; }
}
