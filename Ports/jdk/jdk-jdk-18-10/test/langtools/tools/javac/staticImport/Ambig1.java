/*
 * @test /nodynamiccopyright/
 * @bug 4929736
 * @summary Missing ambiguity error when two methods are equally specific
 * @author gafter
 *
 * @compile/fail/ref=Ambig1.out -XDrawDiagnostics   Ambig1.java
 */

package ambig1;

import static ambig1.A.f;
import static ambig1.B.f;

class A {
    static void f(int i) {}
}
class B {
    static void f(int i) {}
}

class Main {
    void g() {
        f(2);
    }
}
