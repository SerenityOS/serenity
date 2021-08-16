/*
 * @test /nodynamiccopyright/
 * @bug     6638712 6730468
 * @author  mcimadamore
 * @summary Inference with wildcard types causes selection of inapplicable method
 * @compile/fail/ref=T6638712d.out -XDrawDiagnostics T6638712d.java
 */

import java.util.*;

public class T6638712d {

    <U> U m(U u, List<List<U>> list) { return null; }

    void test(List<List<String>> lls) {
        m(1, lls);
    }
}
