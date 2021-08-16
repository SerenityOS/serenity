/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247955 8247957
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-accessibility AccessibilityTest.java
 * @run main DocLintTester -ref AccessibilityTest.out AccessibilityTest.java
 */

/** */
public class AccessibilityTest {
    /**
     * <h1> ... </h1>
     */
    public class Bad_H1 { }

    /**
     * <h3> ... </h3>
     */
    public class Missing_H2 { }

    /**
     * <h2> ... </h2>
     * <h4> ... </h4>
     */
    public class Missing_H3 { }

    /**
     * <h2> ... </h2>
     */
    public void bad_h2() { }

    /**
     * <h4> ... </h4>
     */
    public void missing_h3() { }

    /**
     * <h3> ... </h3>
     * <h5> ... </h5>
     */
    public void missing_h4() { }

    /**
     * <img src="x.jpg">
     */
    public void missing_alt() { }

    /**
     * <table><caption>ok</caption><tr><th>head<tr><td>data</table>
     */
    public void table_with_caption() { }

    /**
     * <table><tr><th>head<tr><td>data</table>
     */
    public void table_without_caption() { }

    /**
     * <table role="presentation"><tr><th>head<tr><td>data</table>
     */
    public void table_presentation() { }

}

