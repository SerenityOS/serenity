/*
 * @test /nodynamiccopyright/
 * @bug 7020044 8062373
 *
 * @summary  Check that diamond is not allowed with anonymous inner class expressions at source < 9
 * @author Maurizio Cimadamore
 * @compile/fail/ref=Neg09a.out Neg09a.java -source 8 -XDrawDiagnostics -Xlint:-options
 *
 */

class Neg09a {
    class Member<X> {}

    void testSimple() {
        Member<?> m1 = new Member<>() {};
    }
}
