/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that lambda conversion does not allow boxing of lambda parameters
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=LambdaConv10.out -XDrawDiagnostics LambdaConv10.java
 */

class LambdaConv10 {

    interface Method1<R, A1> { public R call( A1 a1 ); }

    public static void main( final String... notUsed ) {
        Method1<Integer,Integer> m1 = (int i) -> 2 * i;
    }
}
