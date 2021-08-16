/*
 * @test /nodynamiccopyright/
 * @bug 8081820
 * @summary Validate parameter names uniqueness
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-reference DuplicateParamTest.java
 * @run main DocLintTester -ref DuplicateParamTest.out DuplicateParamTest.java
 */

/** . */
public class DuplicateParamTest {

    /**
     * Test.
     *
     * @param s one
     * @param s two
     * @param s three
     *
     * @return number
     */
    public static int Test(String s) { return s.length(); }
}
