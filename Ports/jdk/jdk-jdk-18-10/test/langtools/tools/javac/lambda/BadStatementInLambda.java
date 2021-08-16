/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that the compiler emits meaningful diagnostics when the lambda body contains bad statements
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=BadStatementInLambda.out -XDrawDiagnostics BadStatementInLambda.java
 */

class BadStatementInLambda {

    interface SAM{
        Object m();
    }

    SAM t1 = ()-> { null; };
    SAM t2 = ()-> { 1; };
    SAM t3 = ()-> { 1 + 5; };
}
