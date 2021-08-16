/*
 * @test /nodynamiccopyright/
 * @bug 7034019
 * @summary ClassCastException in javac with conjunction types
 *
 * @compile/fail/ref=T7034019c.out -XDrawDiagnostics T7034019c.java
 */

class T7034019c {
    interface A {
        abstract <T extends Number> T foo();
    }

    interface B {
        abstract <T> T foo();
    }

    static class C<T extends A & B> {
        void test(T x) {
            x.foo();
        }
    }
}
