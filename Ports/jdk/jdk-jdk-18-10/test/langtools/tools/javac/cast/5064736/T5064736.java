/*
 * @test    /nodynamiccopyright/
 * @bug     5064736
 * @summary Incompatible types are cast without error
 * @compile/fail/ref=T5064736.out -XDrawDiagnostics  T5064736.java
 */

public class T5064736 {
    class A {}
    class B extends A {}

    public class Foo<T> {
        public <U extends B> void foo(Foo<? super A> param) {
            Foo<U> foo = (Foo<U>)param;
        }
    }
}
