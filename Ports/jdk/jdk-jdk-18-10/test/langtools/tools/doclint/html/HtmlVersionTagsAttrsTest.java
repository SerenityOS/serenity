/*
 * @test /nodynamiccopyright/
 * @bug 8072945 8247957 8266856
 * @summary test tags and attributes specific to the output HTML version
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmaxerrs 200 -ref HtmlVersionTagsAttrsTest.out HtmlVersionTagsAttrsTest.java
 */

/**
 * Test HTML tags and attributes based on the output HTML version option.
 */
public class HtmlVersionTagsAttrsTest {
    /**
     * <a rev="help" href="rev_test.html">Help Page</a>
     * <a charset="UTF-8" href="charset_test.html">Test page</a>
     * <a href="shape_test.html" shape="poly" coords="10,30,56,142">Location</a>
     * <img name="name_test" alt="alt">
     * <table>
     * <tr><th axis="desc">Description</th></tr>
     * <tr><td axis="desc" abbr="abbr_test" scope="row">Axis_Test</td></tr>
     * </table>
     * <table summary="summary_test"><tr><td>Test Row</td></tr></table>
     *
     * <table align="left" bgcolor="#EAEAEA" cellpadding="10" cellspacing="2" frame="box" rules="rows" width="200">
     * <caption align="center">Test table, caption, col, colgroup, tbody,
     * td, tfoot, th, thead and tr Align attribute</caption>
     * <colgroup align="char" char="." charoff="2" valign="top" width="200">
     * <col align="center" valign="top" width="200">
     * <col align="char" char="." charoff="2">
     * </colgroup>
     * <thead align="char" char="." charoff="2" valign="top">
     * <tr align="char" char="." charoff="2" bgcolor="#EAEAEA" valign="top">
     * <th align="char" char="." charoff="2" bgcolor="#EAEAEA" height="200" valign="top" width="200" nowrap>HeadCol1</th>
     * <th>HeadCol2</th>
     * </tr>
     * </thead>
     * <tfoot align="char" char="." charoff="2" valign="top">
     * <tr>
     * <td>FootCol1</td>
     * <td>FootCol2</td>
     * </tr>
     * </tfoot>
     * <tbody align="char" char="." charoff="2" valign="top">
     * <tr>
     * <td align="char" char="." charoff="2" bgcolor="#EAEAEA" height="200" valign="top" width="200" nowrap>BodyCol1</td>
     * <td>BodyCol2</td>
     * </tr>
     * </tbody>
     * </table>
     * <br clear="left">
     * <ol compact>
     * <li>Test list</li>
     * <li>Another item</li>
     * </ol>
     * <ul type="circle" compact>
     * <li type="square">Test list</li>
     * <li>Another item</li>
     * </ul>
     * <dl compact>
     * <dt>Test list</dt>
     * <dd>Test Description</dd>
     * </dl>
     * <img src="testImg.jpg" alt="imgTest" hspace="10" vspace="10" border="0">
     * <hr size="20" noshade>
     * <pre width="25">Test Pre</pre>
     * <a name="AnchorTest">Anchor Test</a>
     * <table border="0">
     * <tr><td>Test border</td></tr>
     * </table>
     */
    public void notSupportedAttrs_html5() { }

    /**
     * <ol reversed="reversed">
     * <li>First</li>
     * <li>Second</li>
     * <li>Third</li>
     * </ol>
     * <img src="testImg.jpg" alt="imgTest" crossorigin="anonymous">
     * <div aria-labelledby="Topics" aria-describedby="t1">
     * <h4 id="Topics">Topics</h4>
     * <p id="t1">Aria attribute test</p>
     * <p id="t2" aria-label="Label">Label test</p>
     * </div>
     */
    public void SupportedAttrs_html5() { }

    /**
     * <p><big>Bigger text test</big></p>
     * <center>Center text test</center>
     * <font size="3">Font test</font>
     * <p>Text <strike>strike</strike></p>
     * <p><tt>Teletype text</tt></p>
     * <section>
     * <hgroup>
     * <h4>Section</h4>
     * <h5> Another heading</h5>
     * </hgroup>
     * hgroup no longer supported in HTML5.
     * </section>
     * <details>
     * <summary>Summary</summary>
     * <p>Details and Summary no longer supported in HTML5</p>
     * </details>
     */
    public void notSupportedTags_html5() { }

    /**
     * <section>
     * <p>Testing section tag</p>
     * <h4>Section</h4>
     * Section text.
     * </section>
     * <article>
     * <p>Testing article tag</p>
     * <h5>Article</h5>
     * Article text.
     * </article>
     * <header>
     * <nav>Navigation</nav>
     * Testing header
     * </header>
     * <footer>
     * <nav>Navigation</nav>
     * Testing footer
     * </footer>
     * <main>
     * Main content
     * </main>
     * <aside>
     * <h4>Test aside</h4>
     * <p>Description</p>
     * </aside>
     * <ul>
     * <li>Testing<bdi>BDI</bdi></li>
     * </ul>
     * <figure>
     * <img src="testImg.jpg" alt="imgTest">
     * <figcaption>Fig. 1</figcaption>
     * </figure>
     * <p><mark>Marked</mark> text test</p>
     * <nav>
     * <ul>
     * <li>Nav item 1</li>
     * <li>Nav item 2</li>
     * </ul>
     * </nav>
     * <template id="testTemplate">
     * <div class="desc">Desc</div>
     * </template>
     * <p>Test current time is <time>10:00</time> at night</p>
     * <p>Test<wbr>text</p>
     */
    public void SupportedTags_html5() { }

    /**
     * <section>
     * <p>Invalid use of section tag</p>
     * </section>
     * <article>
     * <p>Invalid use of article tag</p>
     * </article>
     * <header>
     * <header>
     * Invalid nested header
     * </header>
     * <footer>
     * Invalid nested footer
     * </footer>
     * <main>
     * Invalid nested main
     * </main>
     * Invalid use of header
     * </header>
     * <footer>
     * <header>
     * Invalid nested header
     * </header>
     * <footer>
     * Invalid nested footer
     * </footer>
     * <main>
     * Invalid nested main
     * </main>
     * Invalid use of footer
     * </footer>
     * <table border="2">
     * <tr><td>Test border</td></tr>
     * </table>
     */
    public void invalidUsage() { }

    /**
     * <header role="banner">Main text</header>
     * <div role="navigation">
     * <ul><li>Test Nav</li></ul>
     * </div>
     * <table border="1">
     * <tr><td>Test border</td></tr>
     * </table>
     * <table border="">
     * <tr><td>Test border</td></tr>
     * </table>
     */
    public void validUsage() { }
}
