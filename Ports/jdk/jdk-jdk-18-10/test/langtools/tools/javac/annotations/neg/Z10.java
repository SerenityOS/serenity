/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z10.out -XDrawDiagnostics  Z10.java
 */

@interface An {
    int[][] a();
 }
