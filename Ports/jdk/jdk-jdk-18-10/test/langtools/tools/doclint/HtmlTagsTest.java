/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247957
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-html HtmlTagsTest.java
 * @run main DocLintTester -ref HtmlTagsTest.out HtmlTagsTest.java
 */

/** */
public class HtmlTagsTest {
    /**
     * <xyz> ... </xyz>
     */
    public void unknownTag1() { }

    /**
     * <div> <xyz> </div>
     */
    public void unknownTag2() { }

    /**
     * <br/>
     */
    public void selfClosingTag() { }

    /**
     * <html>
     */
    public void not_allowed() { }

    /**
     * <span> <p> </span>
     */
    public void not_allowed_inline() { }

    /**
     * {@link java.lang.String <p> }
     * {@link java.lang.String <p> }
     */
    public void not_allowed_inline_2() { }

    /**
     * <img src="any.jpg" alt="alt"> </img>
     */
    public void end_not_allowed() { }

    /**
     * <i> <b> </i>
     */
    public void start_not_matched() { }

    /**
     * <i> </b> </i>
     */
    public void end_unexpected() { }

    /**
     * <ul> text <li> ... </li> </ul>
     */
    public void text_not_allowed() { }

    /**
     * <ul> <b>text</b> <li> ... </li> </ul>
     */
    public void inline_not_allowed() { }


}

