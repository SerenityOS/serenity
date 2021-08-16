/**
 * @test /nodynamiccopyright/
 * @bug 6707032
 * @summary Verify that \\@SuppressWarnings("divzero") works for constant initializers
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 * @build VerifySuppressWarnings
 * @compile/ref=T6707032.out -XDrawDiagnostics -Xlint:divzero T6707032.java
 * @run main VerifySuppressWarnings T6707032.java
 */

public class T6707032 {
    public static final int D1 = T6707032b.D0;
    public static final int D2 = 1/0;
}

class T6707032b {
    public static final int D0 = 1/0;
    public static final int D3 = T6707032.D2;
}
