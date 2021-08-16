/*
 * @test /nodynamiccopyright/
 * @bug     6400189
 * @summary raw types and inference
 * @author  mcimadamore
 * @compile/fail/ref=T6400189b.out T6400189b.java -Xlint:unchecked -XDrawDiagnostics
 */

class T6400189b<T> {

    static class A {
        <T> T m(T6400189b<T> x) {
            return null;
        }
    }

    static class B<T> extends A {
        <T> T m(T6400189b<T> x) {
            return null;
        }
    }

    void test(B b) {
        Integer i = b.m(new T6400189b<Integer>());
    }
}
