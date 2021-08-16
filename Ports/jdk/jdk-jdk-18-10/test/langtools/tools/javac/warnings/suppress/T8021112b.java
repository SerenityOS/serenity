/**
 * @test /nodynamiccopyright/
 * @bug 8021112
 * @summary Verify that \\@SuppressWarnings("unchecked") works correctly for lazy attrib values
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 * @build VerifySuppressWarnings
 * @compile/ref=T8021112b.out -XDrawDiagnostics -Xlint:unchecked,deprecation,cast T8021112b.java
 * @run main VerifySuppressWarnings T8021112b.java
 */

public class T8021112b {
    public static final String D1 = Dep.D;
    public static final String D2 = "";
    public static final Object[] o = {
        new Object() {
            Dep d;
        }
    };
}

@Deprecated class Dep {
    public static final String D = T8021112b.D2;
}
