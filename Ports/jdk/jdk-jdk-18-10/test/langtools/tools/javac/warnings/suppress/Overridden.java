/**
 * @test /nodynamiccopyright/
 * @bug 8033421
 * @summary Check that \\@SuppressWarnings works properly when overriding deprecated method.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 * @build VerifySuppressWarnings
 * @compile/ref=Overridden.out -XDrawDiagnostics -Xlint:deprecation Overridden.java
 * @run main VerifySuppressWarnings Overridden.java
 */

public class Overridden implements Interface {
    public void test() { }
}

interface Interface {
    @Deprecated void test();
}
