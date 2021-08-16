/*
 * @test /nodynamiccopyright/
 * @bug 5023177
 * @summary One can refer static, const static variables from instance initializers of enum
 * @author gafter
 * @compile/fail/ref=DA3.out -XDrawDiagnostics DA3.java
 */

enum T3 {
    ;
    static int N = 12;
    T3() {
        int M = N;
    }
}
