/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247815
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-missing EmptyParamTest.java
 * @run main DocLintTester -Xmsgs:missing -ref EmptyParamTest.out EmptyParamTest.java
 */

/** . */
public class EmptyParamTest {
    /** @param i */
    void emptyParam(int i) { }
}
