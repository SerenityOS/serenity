/*
 * @test /nodynamiccopyright/
 * @bug 8258957
 * @summary DocLint: check for HTML start element at end of body
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-html EmptyTagsTest.java
 * @run main DocLintTester -Xmsgs:html -ref EmptyTagsTest.out EmptyTagsTest.java
 */

/** . */
public class EmptyTagsTest {
    /**
     * Comment. <p>
     */
    void simpleTrailing() { }

    /**
     * Comment. <p>
     * <ul>
     *     <li>Item.
     * </ul>
     */
    void beforeBlock() { }

    /**
     * Comment. <p>
     * @since 1.0
     */
    void beforeTag() { }

    /**
     * Comment.
     * <ul>
     *     <li>Item. <p>
     * </ul>
     */
    void inBlock() { }

    /**
     * Comment.
     * @author J. Duke<p>
     */
    void inTag() { }
}
