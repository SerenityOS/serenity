/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z9.out -XDrawDiagnostics  Z9.java
 */

@interface An {
    <T> int x();
}
