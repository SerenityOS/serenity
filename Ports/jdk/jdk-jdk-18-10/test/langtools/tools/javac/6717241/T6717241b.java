/**
 * @test /nodynamiccopyright/
 * @bug     6717241
 * @summary some diagnostic argument is prematurely converted into a String object
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=T6717241b.out -XDrawDiagnostics T6717241b.java
 */

class T6717241b {
    void test() {
        //this will generate a 'cant.resolve.location'
        Object o = v;
        //this will generate a 'cant.resolve.location.args'
        m1(1, "");
        //this will generate a 'cant.resolve.location.args.params'
        T6717241b.<Integer,Double>m2(1, "");
    }
}
