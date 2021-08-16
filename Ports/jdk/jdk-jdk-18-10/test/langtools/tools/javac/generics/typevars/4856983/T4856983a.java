/*
 * @test /nodynamiccopyright/
 * @bug 4856983
 * @summary (crash) mutually f-bounded type vars with multiple bounds may crash javac
 * @author Peter von der Ah\u00e9
 * @compile/fail/ref=T4856983a.out -XDrawDiagnostics T4856983a.java
 */

interface I1 { Number m(); }
interface I2 { String m(); }

public class T4856983a {
    <T extends I1 & I2> T f() { return null; }
}
