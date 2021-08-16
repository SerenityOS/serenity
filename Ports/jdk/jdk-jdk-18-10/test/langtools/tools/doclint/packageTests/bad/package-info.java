/*
 * @test /nodynamiccopyright/
 * @bug 8020664 8021215
 * @summary doclint gives incorrect warnings on normal package statements
 * @library ../..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref package-info.out package-info.java
 * @compile/fail/ref=package-info.javac.out -XDrawDiagnostics -Werror -Xdoclint:all package-info.java
 */

// missing comment
package bad;
