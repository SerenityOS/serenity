/*
 * @test /nodynamiccopyright/
 * @bug 8033483
 * @summary Should ignore nested lambda bodies during overload resolution
 * @compile/fail/ref=IgnoreLambdaBodyDuringResolutionTest2.out -XDrawDiagnostics IgnoreLambdaBodyDuringResolutionTest2.java
 */

class IgnoreLambdaBodyDuringResolutionTest2 {
    interface SAM<S> {
        boolean test(S t);
    }

    <I, T extends I> I bar(final T l) {
        return null;
    }

    class D<D1, D2> {
        void foo() {
            m(bar(e -> false));
        }

        void m(Class<D1> arg) {}
        void m(SAM<D2> arg) {}
    }

    class F {
        void foo() {
            m(bar((String e) -> false));
        }

        <F1> void m(Class<F1> arg) {}
        <F2> void m(SAM<F2> arg) {}
    }
}
