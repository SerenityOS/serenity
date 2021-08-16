/*
 * @test /nodynamiccopyright/
 * @summary smoke test for functional interface annotation
 * @compile/fail/ref=FunctionalInterfaceAnno.out -XDrawDiagnostics FunctionalInterfaceAnno.java
 */
class FunctionalInterfaceAnno {
    @FunctionalInterface
    static class A { } //not an interface

    @FunctionalInterface
    static abstract class B { } //not an interface

    @FunctionalInterface
    enum C { } //not an interface

    @FunctionalInterface
    @interface D { } //not an interface

    @FunctionalInterface
    interface E { } //no abstracts

    @FunctionalInterface
    interface F { default void m() { } } //no abstracts

    @FunctionalInterface
    interface G { String toString(); } //no abstracts

    @FunctionalInterface
    interface H { void m(); void n(); } //incompatible abstracts

    @FunctionalInterface
    interface I { void m(); } //ok
}
