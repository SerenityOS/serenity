/*
 * @test /nodynamiccopyright/
 * @bug 8167000
 * @summary Refine handling of multiple maximally specific abstract methods
 * @compile/fail/ref=T8167000b.out -XDrawDiagnostics T8167000b.java
 */
public class T8167000b {
    interface A {
        Integer m() throws Throwable;
    }

    interface B<X extends Throwable> {
        Object m() throws X;
    }

    static abstract class E<T extends Throwable> implements A, B<T> {
        void test() {
            Integer l = m(); //error: unhandled T
        }
    }
}
