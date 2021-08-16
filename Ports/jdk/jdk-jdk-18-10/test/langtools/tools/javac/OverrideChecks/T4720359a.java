/*
 * @test    /nodynamiccopyright/
 * @bug     4720359
 * @summary javac fails to check cross-package hiding
 * @author  gafter
 *
 * @compile/fail/ref=T4720359a.out -XDrawDiagnostics  T4720359a.java T4720359b.java
 */

package p1;
public class T4720359a {
    static void m() {}
}
class T4720359c extends p2.T4720359b {
    // conflicting return type, even though a.m() not inherited
    public static int m() { return 1; }
}
