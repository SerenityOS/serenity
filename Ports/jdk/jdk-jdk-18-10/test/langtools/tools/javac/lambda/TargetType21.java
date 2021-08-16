/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that candidates with cyclic type-inference are removed from the
 *          set of applicable methods
 * @compile/fail/ref=TargetType21.out -XDrawDiagnostics TargetType21.java
 */

class TargetType21 {
    interface SAM1 {
        String m1(Integer n) throws Exception;
    }

    interface SAM2 {
        void m2(Integer n);
    }

    interface SAM3<R,A> {
        R m3(A n);
    }

    void call(SAM1 sam) { }
    void call(SAM2 sam) { }
    <R,A> void call(SAM3<R,A> sam) { }

    void test() {
        call(x -> { throw new Exception(); }); //ambiguous
        call((Integer x) -> { System.out.println(""); }); //ok (only one is void)
        call((Integer x) -> { return (Object) null; }); //ok (only one returns Object)
        call(x -> { System.out.println(""); }); //ambiguous
        call(x -> { return (Object) null; }); //ambiguous
        call(x -> { return null; }); //ambiguous
    }
}
