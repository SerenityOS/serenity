/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z5.out -XDrawDiagnostics  Z5.java
 */

interface Foo {}

@interface Colored extends Foo {
}
