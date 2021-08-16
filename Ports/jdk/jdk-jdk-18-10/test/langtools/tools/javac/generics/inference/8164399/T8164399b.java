/*
 * @test /nodynamiccopyright/
 * @bug 8164399
 * @summary inference of thrown variable does not work correctly
 * @compile/fail/ref=T8164399b.out -XDrawDiagnostics T8164399b.java
 */
class T8164399b<X extends Throwable> {
    <T extends Throwable> void m(Class<? super T> arg) throws T {}
    <T extends X> void g() throws T {}

    void test() {
        m(RuntimeException.class); // ok
        m(Exception.class); // error
        m(Throwable.class); // ok
        m(java.io.Serializable.class); // error
        m(Object.class); // error
        m(Runnable.class); // error
        T8164399b<? super Exception> x = null;
        x.g(); // expected: ok; actual: error
    }
}
