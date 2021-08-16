/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary negative test for ambiguous defaults
 * @compile/fail/ref=Neg01.out -XDrawDiagnostics Neg01.java
 */

class Neg01 {
    interface IA { default int m() { return Neg01.m1(this); } }
    interface IB { default int m() { return Neg01.m2(this); } }

    static class A implements IA {}
    static class B implements IB {}

    static class AB implements IA, IB {}

    static int m1(IA a) { return 0; }
    static int m2(IB b) { return 0; }
}
