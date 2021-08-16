/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  failure to infer exception thrown types from lambda body causes checked exception to be skipped
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=TargetType13.out -XDlambdaInferenceDiags=false -XDrawDiagnostics TargetType13.java
 */

class TargetType13 {

    interface SAM<E extends Throwable> {
       void m(Integer x) throws E;
    }

    static <E extends Throwable> void call(SAM<E> s) throws E { }

    void test() {
        call(i -> { if (i == 2) throw new Exception(); return; });
    }
}
