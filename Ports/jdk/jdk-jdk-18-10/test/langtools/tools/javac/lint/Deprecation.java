/*
 * @test /nodynamiccopyright/
 * @bug 4821359
 * @summary Add -Xlint flag
 * @author gafter
 *
 * @compile/fail/ref=Deprecation.out -XDrawDiagnostics  -Xlint:deprecation -Werror Deprecation.java
 */

/** @deprecated */
class A {
}

class B extends A {
}
