/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z14.out -XDrawDiagnostics  Z14.java
 */

@interface An<T> {
    int a();
}
