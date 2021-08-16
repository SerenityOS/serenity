/*
 * @test /nodynamiccopyright/
 * @bug 8020664 8021215
 * @summary doclint gives incorrect warnings on normal package statements
 * @library ../..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref Test.out Test.java
 * @compile/fail/ref=Test.javac.out -XDrawDiagnostics -Werror -Xdoclint:all Test.java
 */

/** Unexpected comment */
package bad;

/** */
class Test { }

