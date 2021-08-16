/* @test /nodynamiccopyright/
 * @bug 8026374
 * @summary Cannot use void as a variable type
 * @compile/fail/ref=MethodVoidParameter.out -XDrawDiagnostics MethodVoidParameter.java
 */
public class MethodVoidParameter {
    void method(void v) { }
    void method(void... v) { }
}
