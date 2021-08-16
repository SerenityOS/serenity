/*
 * @test /nodynamiccopyright/
 * @bug     7062745
 * @summary  Regression: difference in overload resolution when two methods are maximally specific
 * @compile/fail/ref=T7062745neg.out -XDrawDiagnostics T7062745neg.java
 */

import java.util.*;

class T7062745neg {
    interface A { List<Number> getList(); }
    interface B { ArrayList getList(); }
    interface AB extends A, B {}

    void test(AB ab) {
        Number n = ab.getList().get(1);
    }
}
