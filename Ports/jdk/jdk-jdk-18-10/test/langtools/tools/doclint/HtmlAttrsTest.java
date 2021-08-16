/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8258916 8247957
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-html HtmlAttrsTest.java
 * @run main DocLintTester -ref HtmlAttrsTest.out HtmlAttrsTest.java
 */

/** */
public class HtmlAttrsTest {
    /**
     * <p xyz> text </p>
     */
    public void unknown() { }

    /**
     * <img name="x" alt="alt">
     */
    public void obsolete() { }

    /**
     * <font size="3"> text </font>
     */
    public void obsolete_use_css() { }

    /**
     * multi-line mailto <a
     * href="mailto:nobody@example.com">nobody</a>
     */
    public void multiline_mailto() { }
}

