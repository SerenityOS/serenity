/*
 * @test /nodynamiccopyright/
 * @bug     6569404
 * @summary Regression: Cannot instantiate an inner class of a type variable
 * @author  mcimadamore
 * @compile/fail/ref=T6569404b.out T6569404b.java -XDrawDiagnostics
 */

class T6569404b {

    static class A<X> {}

    static class B<T extends Outer> extends A<T.Inner> {}

    static class Outer {
        public class Inner {}
    }
}
