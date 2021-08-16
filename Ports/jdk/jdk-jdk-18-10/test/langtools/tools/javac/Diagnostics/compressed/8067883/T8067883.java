/*
 * @test /nodynamiccopyright/
 * @bug 8067883
 * @summary Javac misses some opportunities for diagnostic simplification
 *
 * @compile/fail/ref=T8067883a.out -Xdiags:compact -XDrawDiagnostics T8067883.java
 * @compile/fail/ref=T8067883b.out -Xdiags:verbose -XDrawDiagnostics T8067883.java
 *
 */

import java.util.List;

class T8067883 {
    void testMethod(List<Integer> li) {
        m(null, li);
        m(1, li);
    }

    void testDiamond(List<Integer> li) {
        new Box<>(null, li);
        new Box<>(1, li);
    }

    <Z> void m(List<Z> z, List<String> ls) { }

    static class Box<X> {
        Box(List<X> z, List<String> ls) { }
    }
}
