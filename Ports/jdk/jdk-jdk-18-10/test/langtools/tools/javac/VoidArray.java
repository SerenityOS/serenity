/*
 * @test /nodynamiccopyright/
 * @bug 4034979
 * @summary The compiler should never allow void[] to appear as a type
 *          in a program.
 *
 * @compile/fail/ref=VoidArray.out -XDrawDiagnostics VoidArray.java
 */

public
class VoidArray {
    void[] a = null;

    void[] method2(void[][] x) {
        return null;
    }
}
