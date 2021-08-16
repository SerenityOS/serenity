/*
 * @test /nodynamiccopyright/
 * @bug 4856983
 * @summary (crash) mutually f-bounded type vars with multiple bounds may crash javac
 * @author Peter von der Ah\u00e9
 * @compile/fail/ref=T4856983b.out -XDrawDiagnostics T4856983b.java
 */

interface I1 { Number m(); }
interface I2 { String m(); }

public class T4856983b<T extends I1 & I2> {}
