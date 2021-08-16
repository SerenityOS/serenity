/*
 * @test    /nodynamiccopyright/
 * @bug     5060485
 * @summary The scope of a class type parameter is too wide
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=Compatibility.out -XDrawDiagnostics Compatibility.java
 */

class NumberList<T extends Number> {}

class Test<Y extends Number> {
    static class Y {}
    class Y1<S extends NumberList<Y>> {}
}
