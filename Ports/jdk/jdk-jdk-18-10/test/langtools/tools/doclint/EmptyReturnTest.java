/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247815
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-missing EmptyReturnTest.java
 * @run main DocLintTester -Xmsgs:missing -ref EmptyReturnTest.out EmptyReturnTest.java
 */

/** . */
public class EmptyReturnTest {
    /** @return */
    int emptyReturn() { return 0; }
}
