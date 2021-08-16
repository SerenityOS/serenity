/*
 * @test /nodynamiccopyright/
 * @bug     6638712
 * @author  mcimadamore
 * @summary Inference with wildcard types causes selection of inapplicable method
 * @compile/fail/ref=T6638712a.out -XDrawDiagnostics T6638712a.java
 */

import java.util.*;

class T6638712a {

    <T> Comparator<T> compound(Iterable<? extends Comparator<? super T>> it) { return null; }

    public void test(List<Comparator<?>> x) {
        Comparator<String> c3 = compound(x);
    }
}
