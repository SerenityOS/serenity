/*
 * @test /nodynamiccopyright/
 * @bug 8026963
 * @summary type annotations code crashes for lambdas with void argument
 * @compile/fail/ref=TypeAnnotationsCrashWithErroneousTreeTest.out -XDrawDiagnostics --should-stop=at=FLOW TypeAnnotationsCrashWithErroneousTreeTest.java
 */

public class TypeAnnotationsCrashWithErroneousTreeTest {
    private void t(this) {}
}
