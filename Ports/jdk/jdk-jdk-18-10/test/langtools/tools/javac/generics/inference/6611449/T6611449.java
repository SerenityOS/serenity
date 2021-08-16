/**
 * @test /nodynamiccopyright/
 * @bug 6611449 8078024
 * @summary Internal Error thrown during generic method/constructor invocation
 * @compile/fail/ref=T6611449.out -XDrawDiagnostics T6611449.java
 */
public class T6611449<S> {

    <T extends S> T6611449(T t1) {}

    <T extends S> T6611449(T t1, T t2) {}

    <T extends S> void m1(T t1) {}

    <T extends S> void m2(T t1, T t2) {}

    void test() {
        new T6611449<S>(1);
        new T6611449<S>(1, 1); //internal error: lub is erroneously applied to primitive types
        m1(1);
        m2(1, 1); //internal error: lub is erroneously applied to primitive types
    }
}
