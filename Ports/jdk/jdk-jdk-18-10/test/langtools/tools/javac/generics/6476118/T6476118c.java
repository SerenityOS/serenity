/**
 * @test  /nodynamiccopyright/
 * @bug 6476118 7170058
 * @summary compiler bug causes runtime ClassCastException for generics overloading
 * @compile/fail/ref=T6476118c.out -XDrawDiagnostics T6476118c.java
 */

class T6476118c {
    static class A<T> {
        public void foo(T t) { }
    }

    static class B<T extends Number> extends A<T> {
        public void foo(T t) { }
    }

    static class C extends B<Integer> {
        public void foo(Object o) { }
        public void foo(Number o) { }
    }

    static class D extends C {} //check that no spurious diags generated here!
}
