/*
 * @test /nodynamiccopyright/
 * @bug 4855358
 * @summary add support for JSR 201's static import facility
 * @author gafter
 *
 * @compile/fail/ref=StaticImport2.out -XDrawDiagnostics   StaticImport2.java
 */

package p;

import static p.A.*;
import static p.B.*;

interface A {
    int K = 3;
}
interface B {
    int K = 4;
}

class C {
    void f() {
        int x = K; // ambiguous
    }
}
