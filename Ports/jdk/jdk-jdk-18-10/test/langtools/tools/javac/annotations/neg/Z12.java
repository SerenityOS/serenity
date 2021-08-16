/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z12.out -XDrawDiagnostics  Z12.java
 */

@interface An {
    void a();
}
