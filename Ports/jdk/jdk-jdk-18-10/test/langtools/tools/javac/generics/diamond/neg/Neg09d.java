/*
 * @test /nodynamiccopyright/
 * @bug 7020044 8062373
 *
 * @summary  Check that diamond is not allowed with anonymous inner class expressions at source < 9
 * @author Maurizio Cimadamore
 * @compile/fail/ref=Neg09d.out Neg09d.java -source 8 -XDrawDiagnostics -Xlint:-options
 *
 */

class Neg09d {

    static class Nested<X> {}

    void testQualified() {
        Nested<?> m2 = new Neg09.Nested<>() {};
    }
}
