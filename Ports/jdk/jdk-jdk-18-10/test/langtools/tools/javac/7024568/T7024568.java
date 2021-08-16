/* @test /nodynamiccopyright/
 * @bug 7024568
 * @summary Very long method resolution causing OOM error
 * @compile/fail/ref=T7024568.out -XDrawDiagnostics T7024568.java
 */

class Main {
    void test(Obj o) {
        o.test(0, 0, 0, 0, 0, 0, 0, 0, undefined);
    }
}

interface Test {
    public void test(int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, String str);
    public void test(int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, long l);
}

interface Obj extends Test, A, B, C, D, E {}
interface A extends Test {}
interface B extends A, Test {}
interface C extends A, B, Test {}
interface D extends A, B, C, Test {}
interface E extends A, B, C, D, Test {}
