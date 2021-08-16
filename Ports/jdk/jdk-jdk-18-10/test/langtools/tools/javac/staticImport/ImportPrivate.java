/*
 * @test /nodynamiccopyright/
 * @bug 4979456
 * @summary NPE while compiling static import of inaccessible class member
 * @author gafter
 *
 * @compile/fail/ref=ImportPrivate.out -XDrawDiagnostics   ImportPrivate.java
 */

package importPrivate;

import static importPrivate.A.m;

class A {
    private static int m() {
        return 8;
    }
}

class MyTest {
    public static void main(String argv[]) {
        m();
    }
}
