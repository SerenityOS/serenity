/*
 * @test    /nodynamiccopyright/
 * @bug     4916620
 * @summary legal cast is rejected
 * @author  Christian Plesner Hansen
 * @compile/ref=T4916620.out -XDrawDiagnostics -Xlint:unchecked T4916620.java
 * @compile -Xlint:unchecked T4916620.java
 */

public class T4916620 {
    static class BB<T, S> { }
    static class BD<T> extends BB<T, T> { }

    void f() {
        BD<Number> bd = new BD<Number>();
        BB<? extends Number, ? super Integer> bb = bd;
        Object o = (BD<Number>) bb;
    }
}
