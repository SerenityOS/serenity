/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that candidates with incompatible SAM descriptor args length
            are removed from the set of applicable methods
 * @compile/fail/ref=TargetType22.out -Xlint:unchecked -XDrawDiagnostics TargetType22.java
 */

class TargetType22 {

    interface Sam0 {
        void m();
    }

    interface Sam1<A> {
        void m(A a);
    }

    interface Sam2<A> {
        void m(A a1, A a2);
    }

    interface Sam3<A> {
        void m(A a1, A a2, A a3);
    }

    interface SamX<A> {
        void m(A... as);
    }

    void call(Sam0 s) { }
    void call(Sam1<String> s) { }
    void call(Sam2<String> s) { }
    void call(Sam3<String> s) { }
    void call(SamX<String> s) { }

    void test() {
        call(() -> { });
        call(a1 -> { }); //ambiguous - both call(Sam1) and call(SamX) match
        call((a1, a2) -> { });
        call((a1, a2, a3) -> { });
    }
}
