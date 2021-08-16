/* @test /nodynamiccopyright/
 * @bug 8028235
 * @summary Using void as a lambda parameter should produce sane AST and errors
 * @compile/fail/ref=VoidLambdaParameter.out -XDrawDiagnostics VoidLambdaParameter.java
 */
public class VoidLambdaParameter {
    Runnable r = (void v) -> { };
    I i = (void v) -> { };
    interface I {
        public void v(void v);
    }
}
