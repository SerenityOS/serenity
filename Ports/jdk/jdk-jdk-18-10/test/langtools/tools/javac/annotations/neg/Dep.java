/*
 * @test /nodynamiccopyright/
 * @bug 4903501
 * @summary Please add annotation <at>Deprecated to supplant the javadoc tag
 * @author gafter
 *
 * @compile/fail/ref=Dep.out -XDrawDiagnostics  -Xlint:dep-ann -Werror Dep.java
 */

/** @deprecated */
class Dep {
}
