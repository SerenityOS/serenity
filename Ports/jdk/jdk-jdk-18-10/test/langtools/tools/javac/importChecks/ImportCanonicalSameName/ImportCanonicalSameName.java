/*
 * @test /nodynamiccopyright/
 * @bug 8187247
 * @summary canonical import check compares classes by simple name
 * @author cushon
 *
 * @compile p1/A.java p2/A.java
 * @compile/fail/ref=ImportCanonicalSameName.out -XDrawDiagnostics ImportCanonicalSameName.java
 */

package p1;

import p1.A.I;

class T {
    I i;
}
