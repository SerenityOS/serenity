/**
 * @test /nodynamiccopyright/
 * @bug     6722234 8078024
 * @summary javac diagnostics need better integration with the type-system
 * @author  mcimadamore
 * @compile/fail/ref=T6722234b_1.out -XDrawDiagnostics --diags=formatterOptions=simpleNames T6722234b.java
 * @compile/fail/ref=T6722234b_2.out -XDrawDiagnostics --diags=formatterOptions=simpleNames,where T6722234b.java
 */

import java.util.*;

class T6722234b {
    <T> void m(List<T> l1, List<T> l2) {}

    void test(List<? extends T6722234b> list) {
        m(list, list);
    }
}
