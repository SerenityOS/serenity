/*
 * @test /nodynamiccopyright/
 * @bug 7120463
 * @summary Fix method reference parser support in order to avoid ambiguities
 * @compile/fail/ref=T7120463.out -XDrawDiagnostics T7120463.java
 */

class T7120463 {
    void test() { that(i < len, "oopmap"); }
    void that(int i, String s) { };
}
