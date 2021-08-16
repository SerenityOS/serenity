/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247815
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-missing EmptyAuthorTest.java
 * @run main DocLintTester -Xmsgs:missing -ref EmptyAuthorTest.out EmptyAuthorTest.java
 */

/** @author */
public class EmptyAuthorTest {
}
