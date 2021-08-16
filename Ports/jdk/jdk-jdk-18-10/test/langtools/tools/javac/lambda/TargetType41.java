/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  out-of-order method checking should check as many arguments as possible
 * @compile/fail/ref=TargetType41.out -XDrawDiagnostics TargetType41.java
 */

class TargetType41 {
    <X> void m(String s, java.util.List<String> lx) { }

    void test() {
        m(1, new java.util.ArrayList<>());
    }
}
