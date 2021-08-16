/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that type-checking fails because of recursive analysis of stuck expressions
  * @compile/fail/ref=TargetType39.out -XDrawDiagnostics TargetType39.java
 */
class TargetType39 {

    interface I { }

    interface SAM<A, R> {
        R m(A a);
    }

    <U, V> void call(SAM<U, V> s) { }

    void test(boolean cond, SAM<String, Void> ssv) {
        call(cond ? x-> null : ssv);
        call((String s)-> cond ? x-> null : ssv);
    }
}
