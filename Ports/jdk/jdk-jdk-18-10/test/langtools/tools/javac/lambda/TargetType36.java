/*
 * @test /nodynamiccopyright/
 * @bug 8003280 8013404
 * @summary Add lambda tests
 *  check that target type of cast is not propagated to conditional subexpressions
 * @compile/fail/ref=TargetType36.out -XDrawDiagnostics TargetType36.java
 */
class TargetType36 { //awaits spec wording on cast vs. poly

    interface SAM {
       int m(int i, int j);
    }

    void test() {
        SAM s1 = (SAM)((a,b)->a+b);
        SAM s2 = (SAM)(true? (SAM)((a,b)->a+b) : (SAM)((a,b)->a+b));
        SAM s3 = (SAM)(true? (a,b)->a+b : (a,b)->a+b);
    }
}
