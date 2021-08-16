/*
 * @test /nodynamiccopyright/
 * @bug     7007615 7170058
 * @summary java_util/generics/phase2/NameClashTest02 fails since jdk7/pit/b123.
 * @author  mcimadamore
 * @compile/fail/ref=T7007615.out -XDrawDiagnostics T7007615.java
 */

class T6985719a {
    class AX<T extends Number> {
        void foo(T t) { }
    }

    class BX<S extends Integer> extends AX<S> {
        @Override
        void foo(S t) { }
        void bar(BX bx){}
    }

    class DX extends BX<Integer> {
        void foo(Number t) { }
        void bar(BX<?> bx) { }

        @Override
        void foo(Integer t) { }
    }
}
