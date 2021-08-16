/*
 * @test /nodynamiccopyright/
 * @bug 5017254
 * @summary compiler fails to shadow inapplicable method with static import
 * @author gafter
 *
 * @compile/fail/ref=Shadow.out -XDrawDiagnostics   Shadow.java
 */

package shadow;

import static shadow.T1.*;
import static shadow.T2.m1;

class T1 {
    public static void m1() {}
}

class T2 {
    public static void m1(int i) {}
}

class Test {
    void foo() {
        m1(); // <-- is this an error?
    }
}
