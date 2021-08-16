/*
 * @test /nodynamiccopyright/
 * @bug 8162576
 * @summary Missing doclint check missing for modules
 * @library ../..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref module-info.out module-info.java
 * @compile/fail/ref=module-info.javac.out -XDrawDiagnostics -Werror -Xlint:-options -Xdoclint:all module-info.java
 */

// missing doc comment
module bad {
}
