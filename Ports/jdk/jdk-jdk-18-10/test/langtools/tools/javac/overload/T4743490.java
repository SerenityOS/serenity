/*
 * @test /nodynamiccopyright/
 * @bug 4743490
 * @summary overloading versus super.f(args) versus interfaces
 * @author gafter
 * @compile/fail/ref=T4743490.out -XDrawDiagnostics  T4743490.java
 */

class T4743490 {
    static class A {
        public void m(Object o, String s) {}
    }
    interface B {
        void m(String s, Object o);
    }
    static abstract class C extends A implements B {
    }
    static abstract class D extends C {
        void foo() {
            C c = null;
            super.m("", ""); // should be ambiguous.
        }
    }
}
