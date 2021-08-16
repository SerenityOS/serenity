/*
 * @test /nodynamiccopyright/
 * @bug 7139681
 * @summary Enhanced for loop: local variable scope inconsistent with JLS
 *
 * @compile/fail/ref=T7139681neg.out -XDrawDiagnostics T7139681neg.java
 */
class T7139681neg {
    void testArray() {
        for (int a : a) { }
    }

    void testIterable() {
        for (Integer b : b) { }
    }
}
