/*
 * @test /nodynamiccopyright/
 * @bug 8005166
 * @summary Add support for static interface methods
 *          Smoke test for static imports of static interface methods
 * @compile/fail/ref=StaticImport2.out -XDrawDiagnostics -XDallowStaticInterfaceMethods StaticImport2.java
 */

import static pkg.B.*;

class StaticImport2 {
    void test() {
        m();
    }
}
