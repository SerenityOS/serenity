/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z8.out -XDrawDiagnostics  Z8.java
 */

@interface An {
    int x(int y);
}
