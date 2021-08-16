/*
 * @test /nodynamiccopyright/
 * @bug 8167000
 * @summary Refine handling of multiple maximally specific abstract methods
 * @compile/fail/ref=T8167000c.out -XDrawDiagnostics T8167000c.java
 */
public class T8167000c<X extends Throwable> {
    interface A {
        Integer m() throws Throwable;
    }

    interface B<X extends Throwable> {
        Object m() throws X;
    }

    interface E<T extends Throwable> extends A, B<T> { }

    void test() {
        E<X> ex = () -> { throw new Throwable(); };
    }
}
