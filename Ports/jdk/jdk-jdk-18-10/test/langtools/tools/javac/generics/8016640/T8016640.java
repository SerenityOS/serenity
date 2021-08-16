/*
 * @test /nodynamiccopyright/
 * @bug     8016640 8022508
 * @summary compiler hangs if the generics arity of a base class is wrong
 * @compile/fail/ref=T8016640.out -XDrawDiagnostics T8016640.java
 */
class T8016640 {
    static class Foo<X,Y> { }
    static class BadFoo<T> extends Foo<T> { }
    static class SubBadFoo<T> extends BadFoo<T> { }
}
