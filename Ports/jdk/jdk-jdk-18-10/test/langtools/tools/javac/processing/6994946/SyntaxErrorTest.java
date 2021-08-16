/*
 * @test /nodynamiccopyright/
 * @bug 6994946
 * @summary option to specify only syntax errors as unrecoverable
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor TestProcessor
 * @compile/fail/ref=SyntaxErrorTest.out -XDrawDiagnostics                                  -processor TestProcessor SyntaxErrorTest.java
 * @compile/fail/ref=SyntaxErrorTest.out -XDrawDiagnostics -XDonlySyntaxErrorsUnrecoverable -processor TestProcessor SyntaxErrorTest.java
 */

class SyntaxErrorTest {
    int i
}
