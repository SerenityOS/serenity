/*
 * @test /nodynamiccopyright/
 * @bug 8007285
 * @summary AbstractMethodError instead of compile-time error when method reference with super and abstract
 * @compile/fail/ref=MethodReference62.out -XDrawDiagnostics MethodReference62.java
 */
class MethodReference62 {
    interface SAM {
        int m();
    }

    static abstract class Sup {
        abstract int foo() ;
    }

    static abstract class Sub extends Sup {
        abstract int foo() ;
        void test() {
            SAM s = super::foo;
        }
    }
}
