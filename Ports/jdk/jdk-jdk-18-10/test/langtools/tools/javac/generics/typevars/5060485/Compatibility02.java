/*
 * @test    /nodynamiccopyright/
 * @bug     5060485 6977800
 * @summary The scope of a class type parameter is too wide
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=Compatibility02.out -XDrawDiagnostics Compatibility.java
 */

class NumberList<T extends Number> {}

class Test {
    <Y extends Number> void m() {
        static class Y {}
        class Y1<S extends NumberList<Y>> {}
    }
}
