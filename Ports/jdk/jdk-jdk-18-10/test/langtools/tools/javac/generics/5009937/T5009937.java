/*
 * @test /nodynamiccopyright/
 * @bug 5009937
 * @summary hiding versus generics versus binary compatibility
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T5009937.out -XDrawDiagnostics T5009937.java
 */

public class T5009937<X> {
    static class A {
        static void m(T5009937<String> l) {}
    }

    static class B extends A {
        static void m(T5009937<Integer> l) {}
    }
}
