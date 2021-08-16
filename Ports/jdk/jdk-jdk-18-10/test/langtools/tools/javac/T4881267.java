/*
 * @test /nodynamiccopyright/
 * @bug 4881267
 * @summary improve diagnostic for "instanceof T" for type parameter T
 * @compile/fail/ref=T4881267.out -XDrawDiagnostics T4881267.java
 */

class T4881267 {
    <T> void m(Object o) {
        boolean b = o instanceof T;
    }
}
