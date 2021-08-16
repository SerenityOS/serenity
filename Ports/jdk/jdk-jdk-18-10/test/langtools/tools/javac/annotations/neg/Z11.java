/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z11.out -XDrawDiagnostics  Z11.java
 */

class X {}

@interface An {
    X a();
}
