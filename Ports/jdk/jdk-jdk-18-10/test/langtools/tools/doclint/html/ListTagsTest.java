/*
 * @test /nodynamiccopyright/
 * @bug 8006251 8013405 8022173
 * @summary test list tags
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs -ref ListTagsTest.out ListTagsTest.java
 */

/** */
public class ListTagsTest {
    /**
     *  <dl> <dt> abc <dd> def </dl>
     *  <ol> <li> abc </ol>
     *  <ol> <li value="1"> abc </ol>
     *  <ol> <li value> bad </ol>
     *  <ol> <li value="a"> bad </ol>
     *  <ol type="a"> <li> bad </ol>
     *  <ul> <li> abc </ul>
     */
    public void supportedTags() { }
}
