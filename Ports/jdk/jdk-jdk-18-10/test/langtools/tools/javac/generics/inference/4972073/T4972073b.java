/*
 * @test    /nodynamiccopyright/
 * @bug     4972073
 * @summary same interface allowed twice in compound type
 * @compile/fail/ref=T4972073b.out -XDrawDiagnostics  T4972073b.java
 */

public class T4972073b {

    static class D {}

    static interface MyInterface<E> {
        public String foo();
    }

    static class MyClass {}

    static class B<E extends MyClass & MyInterface<E> & MyInterface<E>> extends D {}

}
