/*
 * @test /nodynamiccopyright/
 * @bug 8005166
 * @summary Add support for static interface methods
 *          Smoke test for static imports of static interface methods
 * @compile/fail/ref=StaticImport3.out -XDrawDiagnostics -XDallowStaticInterfaceMethods StaticImport3.java
 */

import static pkg.C.*;

class StaticImport3 {
    void test() {
        m();
    }
}
