/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8026368 8247957
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref TrimmingEmptyTag.out TrimmingEmptyTag.java
 */

// tidy: Warning: trimming empty <.*>

/**
 * <b></b>
 * <table><caption></caption></table>
 * <code></code>
 * <dl></dl>
 * <dl><dt></dt><dd></dd></dl>
 * <i></i>
 * <ol></ol>
 * <p></p>
 * <pre></pre>
 * <span></span>
 * <ul></ul>
 * <ul><li></li></ul>
 */
public class TrimmingEmptyTag {
    /** <p> */
    public void implicitParaEnd_endOfComment() { }
    /** <p> <ul><li>text</ul> */
    public void implicitParaEnd_nextBlockTag() { }
}
