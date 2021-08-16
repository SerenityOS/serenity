/*
 * @test /nodynamiccopyright/
 * @bug 4949303
 * @summary A method returning a raw type cannot override a method returning a generic type
 * @author gafter
 *
 * @compile  UncheckedCovariance.java
 * @compile/fail/ref=UncheckedCovariance.out -XDrawDiagnostics  -Xlint:unchecked -Werror  UncheckedCovariance.java
 */

class UncheckedCovariance {
    static class Box<T> { }
    static class A {
        Box<Integer> f() { return null; }
    }
    static class B extends A {
        Box f() { return null; }
    }
}
