/*
 * @test /nodynamiccopyright/
 * @bug 7034019
 * @summary ClassCastException in javac with conjunction types
 *
 * @compile/fail/ref=T7034019d.out -XDrawDiagnostics T7034019d.java
 */

class T7034019c {
    interface A {
        abstract <T extends Number> T foo();
    }

    interface B {
        abstract <T> T foo();
    }

    static abstract class E implements A,B {
        void test() {
            foo();
        }
    }
}
