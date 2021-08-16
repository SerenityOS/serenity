/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247957
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref AnchorTest.out AnchorTest.java
 */

/** */
public class AnchorTest {
    // tests for <a id=value>

    /**
     * <a id=foo></a>
     */
    public void a_id_foo() { }

    /**
     * <a id=foo></a>
     */
    public void a_id_already_defined() { }

    /**
     * <a id=></a>
     */
    public void a_id_empty() { }

    /**
     * <a id="123 "></a>
     */
    public void a_id_invalid() { }

    /**
     * <a id ></a>
     */
    public void a_id_missing() { }

    // tests for id=value on non-<a> tags

    /**
     * <p id=p_id_foo>text</p>
     */
    public void p_id_foo() { }

    /**
     * <p id=foo>text</p>
     */
    public void p_id_already_defined() { }

    /**
     * <p id=>text</p>
     */
    public void p_id_empty() { }

    /**
     * <p id="123 ">text</p>
     */
    public void p_id_invalid() { }

    /**
     * <p id >text</p>
     */
    public void p_id_missing() { }


}
