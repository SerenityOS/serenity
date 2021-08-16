/**
 * @test /nodynamiccopyright/
 * @bug 8069094
 * @summary Verify that \\@SuppressWarnings("unchecked") works correctly for annotation default values
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 * @build VerifySuppressWarnings
 * @compile/ref=T8069094.out -XDrawDiagnostics -Xlint:unchecked,deprecation,cast T8069094.java
 * @run main VerifySuppressWarnings T8069094.java
 */

@interface T8069094 {
    T8069094A foo() default T8069094A.Bar;
}

@Deprecated
enum T8069094A {
    Bar
}
