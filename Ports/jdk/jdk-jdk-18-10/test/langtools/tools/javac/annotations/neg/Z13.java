/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z13.out -XDrawDiagnostics  Z13.java
 */

@interface An {
    int a() throws NullPointerException;
}
