/*
 * @test /nodynamiccopyright/
 * @bug 7020044 8062373
 *
 * @summary  Check that diamond is not allowed with anonymous inner class expressions at source < 9
 * @author Maurizio Cimadamore
 * @compile/fail/ref=Neg09c.out Neg09c.java -source 8 -XDrawDiagnostics -Xlint:-options
 *
 */

class Neg09c {
    class Member<X> {}

    void testQualified() {
        Member<?> m1 = this.new Member<>() {};
    }
}
