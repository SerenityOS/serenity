/*
 * @test /nodynamiccopyright/
 * @bug 8081820
 * @summary Validate return uniqueness
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-reference DuplicateReturnTest.java
 * @run main DocLintTester -ref DuplicateReturnTest.out DuplicateReturnTest.java
 */

/** . */
public class DuplicateReturnTest {

    /**
     * Test.
     *
     * @param s one
     *
     * @return one
     * @return two
     * @return three
     */
    public static int Test(String s) { return s.length(); }
}
