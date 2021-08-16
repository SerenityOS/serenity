/*
 * @test /nodynamiccopyright/
 * @bug 5023177
 * @summary One can refer static, const static variables from instance initializers of enum
 * @author gafter
 * @compile/fail/ref=DA1.out -XDrawDiagnostics  DA1.java
 */

enum T1 {
    ;
    static int N = 12;
    int M = N;
}
