/*
 * @test /nodynamiccopyright/
 * @bug     7034511 7040883 7041019
 * @summary Loophole in typesafety
 * @compile/fail/ref=T7034511b.out -XDrawDiagnostics T7034511b.java
 */

class T7034511b {
    static class MyList<E> {
        E toArray(E[] e) { return null; }
    }

    void test(MyList<?> ml, Object o[]) {
        ml.toArray(o);
    }
}
