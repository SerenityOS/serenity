/*
 * @test    /nodynamiccopyright/
 * @bug     4720356
 * @summary compiler fails to check cross-package overriding
 * @author  gafter
 *
 * @compile/fail/ref=T4720356a.out -XDrawDiagnostics  T4720356a.java T4720356b.java
 */

package p1;
public class T4720356a {
    void m() {}
}
class T4720356c extends p2.T4720356b {
    // conflicting return type, even though a.m() not inherited
    public int m() { return 1; }
}
