/*
 * @test /nodynamiccopyright/
 * @bug 4821359 4981267
 * @summary Add -Xlint flag
 * @author gafter
 *
 * @compile/fail/ref=FallThrough.out -XDrawDiagnostics  -Xlint:fallthrough -Werror FallThrough.java
 */

class FallThrough {
    int x;
    void f(int i) {
        switch (i) {
        case 0:
            x = 0;
        case 1:
        case 2:
            x = 2;
            break;
        default:
            x = 3;
        }
    }
}
