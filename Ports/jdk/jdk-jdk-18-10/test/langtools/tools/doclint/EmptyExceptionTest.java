/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247815
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-missing EmptyExceptionTest.java
 * @run main DocLintTester -Xmsgs:missing -ref EmptyExceptionTest.out EmptyExceptionTest.java
 */

/** . */
public class EmptyExceptionTest {
    /** @exception NullPointerException */
    void emptyException() throws NullPointerException { }
}
