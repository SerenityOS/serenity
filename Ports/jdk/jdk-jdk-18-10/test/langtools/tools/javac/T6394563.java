/*
 * @test  /nodynamiccopyright/
 * @bug 6394563
 * @summary javac ignores -nowarn switch in 1.5.0_06 for deprecation warnings
 *
 * @compile/ref=T6394563.note.out  -XDrawDiagnostics -nowarn             T6394563.java
 * @compile/ref=T6394563.warn.out  -XDrawDiagnostics -Xlint -nowarn      T6394563.java
 */

class T6394563 {
    void useDeprecated() {
        deprecated.foo();
    }
}

class deprecated {
    /** @deprecated */ static void foo() { }
}
