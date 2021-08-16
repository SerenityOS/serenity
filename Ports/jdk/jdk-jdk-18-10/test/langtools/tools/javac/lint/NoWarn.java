/**
 * @test  /nodynamiccopyright/
 * @bug 6183484
 * @summary verify -nowarn is the same as -Xlint:none
 * @compile/ref=NoWarn1.out -XDrawDiagnostics             NoWarn.java
 * @compile/ref=NoWarn2.out -XDrawDiagnostics -nowarn     NoWarn.java
 * @compile/ref=NoWarn2.out -XDrawDiagnostics -Xlint:none NoWarn.java
 */

class NoWarn {
    void m(Object... args) { }
    void foo() {
        m(null);
    }
}
