/* @test /nodynamiccopyright/
 * @bug 8025246 8247957
 * @summary doclint is showing error on anchor already defined when it's not
 * @library ../..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref package-info.out package-info.java
 * @compile/fail/ref=package-info.javac.out -XDrawDiagnostics -Werror -Xdoclint:all package-info.java
 */

/**
 * <a id=here>here</a>
 * <a id=here>here again</a>
 * <a name=name>obsolete anchor</a>
 * <a name=name>obsolete anchor again</a>
 */
package p;

