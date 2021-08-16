/*
 * @test    /nodynamiccopyright/
 * @bug     4972073
 * @summary same interface allowed twice in compound type
 * @compile/fail/ref=T4972073a.out -XDrawDiagnostics  T4972073a.java
 */

public class T4972073a {

    static class D {}

    static interface MyInterface<E> {
        public String foo();
    }

    static class MyClass {}

    static class Sun1 extends MyClass implements MyInterface<Sun1>, MyInterface<Sun1> {
        public String foo() {
            return "test";
        }
    }
}
