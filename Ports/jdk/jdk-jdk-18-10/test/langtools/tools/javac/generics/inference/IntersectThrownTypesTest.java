/*
 * @test /nodynamiccopyright/
 * @bug 8148930
 * @summary Incorrect erasure of exceptions in override-equivalent dual interface impl
 * @compile/fail/ref=IntersectThrownTypesTest.out -XDrawDiagnostics IntersectThrownTypesTest.java
 */

public class IntersectThrownTypesTest {

    interface S1 {
        <K extends Exception> void run(Class<K> clazz) throws K;
    }

    interface S2 {
        <K extends Exception> void run(Class<K> clazz) throws K;
    }

    interface S extends S1, S2 {}

    public void foo(S1 s) {
        s.run(java.io.IOException.class);
    }

    public void foo(S s) {
        s.run(java.io.IOException.class);
    }

}
