/*
 * @test /nodynamiccopyright/
 * @bug 6994946
 * @summary option to specify only syntax errors as unrecoverable
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor TestProcessor
 * @compile/fail/ref=SemanticErrorTest.1.out -XDrawDiagnostics                                  -processor TestProcessor SemanticErrorTest.java
 * @compile/fail/ref=SemanticErrorTest.2.out -XDrawDiagnostics -XDonlySyntaxErrorsUnrecoverable -processor TestProcessor SemanticErrorTest.java
 */

class SemanticErrorTest implements Runnable, Runnable {
    public void run() { }
}
