/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  compiler silently crashes when void method is passed as argument in overloaded call site
 * @compile/fail/ref=TargetType40.out -XDrawDiagnostics TargetType40.java
 */

class TargetType40 {
    void m(String s) { }
    void m(Integer i) { }

    void void_method() {}

    void test() {
       m(void_method());
    }
}
