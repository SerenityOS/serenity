/*
 * @test /nodynamiccopyright/
 * @bug 4936393 8050021
 * @summary enum switch case labels required to be unqualified.
 * @author gafter
 * @compile/fail/ref=EnumSwitch2.out -XDrawDiagnostics EnumSwitch2.java
 */

enum E1 { a, b, c, d, e }

class EnumSwitch2 {
    void f(E1 e1) {
        switch (e1) {
        case E1.a:
        case E1.d:
        default:
            break;
        }
    }
}
