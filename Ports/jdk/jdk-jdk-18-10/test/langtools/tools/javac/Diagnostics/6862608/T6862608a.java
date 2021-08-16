/**
 * @test /nodynamiccopyright/
 * @bug     6862608
 * @summary rich diagnostic sometimes contain wrong type variable numbering
 * @author  mcimadamore
 * @compile/fail/ref=T6862608a.out -XDrawDiagnostics --diags=formatterOptions=disambiguateTvars,where T6862608a.java
 */


import java.util.*;

class T6862608a {

    <T> Comparator<T> compound(Iterable<? extends Comparator<? super T>> it) {
        return null;
    }

    public void test(List<Comparator<?>> x) {
        Comparator<String> c3 = compound(x);
    }
}
