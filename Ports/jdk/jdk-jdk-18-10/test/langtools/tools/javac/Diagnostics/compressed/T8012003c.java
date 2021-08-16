/**
 * @test /nodynamiccopyright/
 * @bug     8012003
 * @summary Method diagnostics resolution need to be simplified in some cases
 *          test simplification of lambda type-checking error leading to resolution failure
 * @compile/fail/ref=T8012003c.out -XDrawDiagnostics -Xdiags:compact T8012003c.java
 */

class T8012003c {

    interface I {
        void m(P p);
    }

    void m(I i) { }

    void test() {
        m(p->p.m());
    }
}

class P {
    private void m() { }
}
