/*
 * @test /nodynamiccopyright/
 * @bug 4916567
 * @summary integrate improved wildcard substitution from CPH
 * @author gafter
 *
 * @compile/fail/ref=ParamCast.out -XDrawDiagnostics ParamCast.java
 */

class A<T> {}
class B<S, T> extends A<T> {}

class Main {
    void f(A<String> as) {
        Object o = (B<?, Integer>) as;
    }
}
