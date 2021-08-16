/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that lambda expression body (when not a block) cannot be void
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=LambdaExprNotVoid.out -XDlambdaInferenceDiags=false -XDrawDiagnostics LambdaExprNotVoid.java
 */

class LambdaExpr05 {

    interface SAM { void foo(int i); }

    SAM s1 = i -> i * 2;
    SAM s2 = i -> 2 * i;
}
