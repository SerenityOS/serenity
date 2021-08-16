/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  target typing in assignment context
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=TargetType04.out -XDrawDiagnostics TargetType04.java
 */
class TargetType04 {

    interface S<X extends Number, Y extends Number> {
       Y m(X x);
    }

    S<Integer, Integer> s1 = i -> { return i; }; //ok
    S<Double, Integer> s2 = i -> { return i; }; //no
    S<Integer, Double> s3 = i -> { return i; }; //no
}
