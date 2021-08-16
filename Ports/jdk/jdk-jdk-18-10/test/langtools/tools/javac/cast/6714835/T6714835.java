/*
 * @test /nodynamiccopyright/
 * @author mcimadamore
 * @bug     6714835
 * @summary Safe cast is rejected (with warning) by javac
 * @compile/fail/ref=T6714835.out -Xlint:unchecked -Werror -XDrawDiagnostics T6714835.java
 */

import java.util.*;

class T6714835 {
    void cast1(Iterable<? extends Integer> x) {
        Collection<? extends Number> x1 = (Collection<? extends Number>)x; //ok
        Collection<? super Integer> x2 = (Collection<? super Integer>)x; //warn
    }

    void cast2(Iterable<? super Number> x) {
        Collection<? super Integer> x1 = (Collection<? super Integer>)x; //ok
        Collection<? extends Number> x2 = (Collection<? extends Number>)x; //warn
    }
}
