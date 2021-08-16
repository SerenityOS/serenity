/*
 * @test /nodynamiccopyright/
 * @bug 8156180
 * @summary javac accepts code that violates JLS chapter 16
 *
 * @compile/fail/ref=T8156180.out -XDrawDiagnostics T8156180.java
 */

class T8156180 {
    public final int a1, b1, c1, d1;
    public int a2, b2, c2, d2;

    T8156180(int value) {
        a2 = this.a1;
        b2 = (this).b1;
        c2 = ((this)).c1;
        d2 = (((this))).d1;
        a1 = value;
        b1 = value;
        c1 = value;
        d1 = value;
    }
}
