/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that the compiler emits meaningful diagnostics when the lambda body contains bad statements
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=BadStatementInLambda02.out -XDrawDiagnostics BadStatementInLambda02.java
 */

class BadStatementInLambda02 {

    interface SAM {
        void m();
    }

    { call(()-> { System.out.println(new NonExistentClass() + ""); }); }

    void call(SAM s) { }
}
