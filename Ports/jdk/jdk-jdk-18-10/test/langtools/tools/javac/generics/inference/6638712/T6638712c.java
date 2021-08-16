/*
 * @test /nodynamiccopyright/
 * @bug     6638712 6707034
 * @author  mcimadamore
 * @summary Inference with wildcard types causes selection of inapplicable method
 * @compile/fail/ref=T6638712c.out -XDrawDiagnostics T6638712c.java
 */

import java.util.*;

class T6638712c {

    <T> T sort(T[] a, Comparator<? super T> c) { return null; }

    void test(Enum[] e, Comparator<Enum<?>> comp) {
        sort(e, comp);
    }
}
