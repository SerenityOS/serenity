/*
 * @test /nodynamiccopyright/
 * @bug 8242802
 * @summary Verify javac does not crash while checking for equals/hashCode overrides
 * @compile/fail/ref=InvalidAnonymous.out -XDrawDiagnostics InvalidAnonymous.java
 * @compile/fail/ref=InvalidAnonymous.out -XDrawDiagnostics -Xlint:overrides InvalidAnonymous.java
 */
public class InvalidAnonymous {
    private void t() {
        new Undefined() {};
    }
}
