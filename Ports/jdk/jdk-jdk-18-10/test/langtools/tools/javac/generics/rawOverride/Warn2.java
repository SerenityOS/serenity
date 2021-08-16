/*
 * @test /nodynamiccopyright/
 * @bug 5073079
 * @summary Allow unchecked override of generified methods in
 * parameterless classes
 * @author Peter von der Ah\u00e9
 *
 * @compile Warn2.java
 * @compile/fail/ref=Warn2.out -XDrawDiagnostics -Xlint:unchecked -Werror Warn2.java
 */

interface I3 {
    <T> T foo();
}

class C3 implements I3 {
    public Object foo() { return null; }
}
