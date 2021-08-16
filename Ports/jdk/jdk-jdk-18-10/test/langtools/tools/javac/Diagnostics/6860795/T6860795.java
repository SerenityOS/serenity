/**
 * @test /nodynamiccopyright/
 * @bug     6860795
 * @summary NullPointerException when compiling a negative java source
 * @author  mcimadamore
 * @compile/fail/ref=T6860795.out -XDrawDiagnostics  T6860795.java
 */

class Test {
    void foo(float x, int x) {}
}
