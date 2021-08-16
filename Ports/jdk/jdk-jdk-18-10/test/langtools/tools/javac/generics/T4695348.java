/*
 * @test /nodynamiccopyright/
 * @bug 4695348
 * @summary generics: compiler allows ref to type bounds in static members
 * @author gafter
 *
 * @compile/fail/ref=T4695348.out -XDrawDiagnostics  T4695348.java
 */

class T4695348<T> {
    static T x = null;
}
