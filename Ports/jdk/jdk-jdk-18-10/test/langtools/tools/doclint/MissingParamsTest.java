/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-missing MissingParamsTest.java
 * @run main DocLintTester -Xmsgs:missing -ref MissingParamsTest.out MissingParamsTest.java
 */

/** . */
public class MissingParamsTest {
    /** */
    MissingParamsTest(int param) { }

    /** */
    <T> MissingParamsTest() { }

    /** */
    void missingParam(int param) { }

    /** */
    <T> void missingTyparam() { }
}
