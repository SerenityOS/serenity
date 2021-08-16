/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  complex case of cyclic type inference (lambda returned where inference var expected)
 * @compile/fail/ref=TargetType27.out -XDrawDiagnostics TargetType27.java
 * @compile/fail/ref=TargetType27.out -XDrawDiagnostics TargetType27.java
 */

class TargetType27 {
    interface F<X, Y>  {
        Y f(X a);
    }

    <A, R> F<A, R> m(F<A, R>  f) { return null; }

    void test() {
        m((String s1) ->  (String s2) -> Integer.valueOf(1));
    }
}
