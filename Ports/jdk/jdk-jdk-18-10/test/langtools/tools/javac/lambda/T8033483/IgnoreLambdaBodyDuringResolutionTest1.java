/*
 * @test /nodynamiccopyright/
 * @bug 8033483
 * @summary Should ignore nested lambda bodies during overload resolution
 * @compile/fail/ref=IgnoreLambdaBodyDuringResolutionTest1.out -XDrawDiagnostics IgnoreLambdaBodyDuringResolutionTest1.java
 */

class IgnoreLambdaBodyDuringResolutionTest1 {
    interface SAM<T> {
        T action(T t);
    }

    <T> T m(SAM<T> op) {
        return null;
    }

    class B {
        B x() {
            return this;
        }
    }

    class C {}

    void foo(B arg) {}
    void foo(C arg) {}

    void bar() {
        foo(m(arg -> new B()));
    }
}
