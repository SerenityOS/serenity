/**
 * @test  /nodynamiccopyright/
 * @bug 6598104 8032211
 * @build p.Dep1 p.Dep2
 * @compile/ref=NestedDeprecation.out -Xlint:deprecation -XDrawDiagnostics NestedDeprecation.java
 */

package p;

import p.Dep1.A;
import static p.Dep1.B;
import static p.Dep1.method;
import static p.Dep1.field;
import p.Dep2.C;
import p.Dep2.D;

class NestedDeprecation {
    Dep1 f1;
    A f2;
    Dep2 f3;
    B f4;
    C f5;
    D f6;

    static {
        method();
        String f = field;
    }
}
