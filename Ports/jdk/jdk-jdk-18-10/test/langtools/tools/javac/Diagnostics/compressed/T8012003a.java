/**
 * @test /nodynamiccopyright/
 * @bug     8012003
 * @summary Method diagnostics resolution need to be simplified in some cases
 *          test general overload resolution simplifications
 * @compile/fail/ref=T8012003a.out -XDrawDiagnostics -Xdiags:compact T8012003a.java
 */

class T8012003a {
    void m1(Integer i) { }

    void m2(Integer i) { }
    void m2(Integer i, Object o) { }

    void m3(Integer i) { }
    void m3(String s) { }

    void test() {
        m1("");
        m1(false ? "" : "");
        m2("");
        m3('x');
    }
}
