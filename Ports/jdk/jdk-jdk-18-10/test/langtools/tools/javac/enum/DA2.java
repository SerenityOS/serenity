/*
 * @test /nodynamiccopyright/
 * @bug 5023177
 * @summary One can refer static, const static variables from instance initializers of enum
 * @author gafter
 * @compile/fail/ref=DA2.out -XDrawDiagnostics DA2.java
 */

enum T2 {
    ;
    static int N = 12;
    {
        int M = N;
    }
}
