/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that abstract methods are compatible with inherited defaults
 * @compile/fail/ref=Neg05.out -XDrawDiagnostics Neg05.java
 */

class Neg05 {
    interface IA1 { default Number m() { return Neg05.m1(this); } }
    interface IA2 extends IA1 { default Integer m() { return Neg05.m2(this); } }
    interface IA3 extends IA2 { Number m(); } //error

    static class C implements IA3{}

    static int m1(IA1 a) { return 0; }
    static int m2(IA2 b) { return 0; }
}
