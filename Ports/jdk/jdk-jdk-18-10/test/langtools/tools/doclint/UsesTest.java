/*
 * @test /nodynamiccopyright/
 * @bug 8160196
 * @summary Module summary page should display information based on "api" or "detail" mode.
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref UsesTest.out UsesTest.java
 */

/**
 * Invalid use of uses in class documentation.
 *
 * @uses NotFound
 */
public class UsesTest {
    /**
     * Invalid use of uses in field documentation
     *
     * @uses NotFound Test description.
     */
    public int invalid_param;

    /**
     * Invalid use of uses in method documentation
     *
     * @uses NotFound Test description.
     */
    public class InvalidParam { }
}
