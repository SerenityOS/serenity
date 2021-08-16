/*
 * @test /nodynamiccopyright/
 * @bug 8006775
 * @summary Package declarations cannot use annotations.
 * @author Werner Dietl
 * @compile/fail/ref=AnnotatedPackage2.out -XDrawDiagnostics AnnotatedPackage2.java
 */

package @A p1.p2;

import java.lang.annotation.*;

class AnnotatedPackage2 { }

@Target(ElementType.TYPE_USE)
@interface A { }
