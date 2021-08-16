/*
 * @test /nodynamiccopyright/
 * @bug 4881269
 * @summary improve diagnostic for ill-formed tokens
 * @compile/fail/ref=T4881269.out -XDrawDiagnostics T4881269.java
 */

public class T4881269 {
    java.io..PrintStream s;
    void m() { System.err..println(); }
    void m(Object.. o) { }
}
