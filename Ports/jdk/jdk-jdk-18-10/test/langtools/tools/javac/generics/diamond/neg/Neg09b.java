/*
 * @test /nodynamiccopyright/
 * @bug 7020044 8062373
 *
 * @summary  Check that diamond is not allowed with anonymous inner class expressions at source < 9
 * @author Maurizio Cimadamore
 * @compile/fail/ref=Neg09b.out Neg09b.java -source 8 -XDrawDiagnostics -Xlint:-options
 *
 */

class Neg09b {

    static class Nested<X> {}

    void testSimple() {
        Nested<?> m2 = new Nested<>() {};
    }

}
