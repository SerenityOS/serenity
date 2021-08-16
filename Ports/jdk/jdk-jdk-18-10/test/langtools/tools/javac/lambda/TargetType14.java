/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that return type is inferred from target type when cyclic inference found
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=TargetType14.out -XDrawDiagnostics TargetType14.java
 */

class TargetType14 {

    interface SAM<X> {
        X m(int i, int j);
    }

    static void test() {
        SAM<Integer> s1 = (i, j) -> i + j;
        m((i, j) -> i + j);
        SAM<Integer> s2 = m2((i, j) -> i + j); //ok
        SAM<Integer> s3 = m2((i, j) -> "" + i + j); //no
    }

    static void m(SAM<Integer> s) { }
    static <X> SAM<X> m2(SAM<X> s) { return null; }
}
