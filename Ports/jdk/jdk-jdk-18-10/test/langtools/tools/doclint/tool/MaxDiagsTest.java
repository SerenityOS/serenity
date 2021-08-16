/*
 * @test /nodynamiccopyright/
 * @bug 8006263
 * @summary Supplementary test cases needed for doclint
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref MaxDiagsTest.out -Xmaxerrs 2 -Xmaxwarns 2 MaxDiagsTest.java
 * @run main DocLintTester -badargs -Xmaxerrs
 * @run main DocLintTester -badargs -Xmaxwarns
 * @run main DocLintTester -badargs -Xmaxerrs two -Xmaxwarns two MaxDiagsTest.java
 */

public class MaxDiagsTest {
    /**
     * &#0; &#0; &#0; &#0;
     */
    public void errors() { }

    /** 4 undocumented signature items */
    public int warnings(int a1, int a2) throws Exception { return 0; }
}
