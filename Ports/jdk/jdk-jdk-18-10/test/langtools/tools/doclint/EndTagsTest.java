/*
 * @test /nodynamiccopyright/
 * @bug 8006236 8247957
 * @summary doclint: structural issue hidden
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-html EndTagsTest.java
 * @run main DocLintTester -ref EndTagsTest.out EndTagsTest.java
 */

/** */
public class EndTagsTest {
    /** <p>  <a id="a1"> text <img alt="image" src="image.png"> </a> </p> */
    public void valid_all() { }

    /** <p>  <a id="a2"> text <img alt="image" src="image.png"> </a> */
    public void valid_omit_optional_close() { }

    /** </a> */
    public void invalid_missing_start() { }

    /** <p> </a> */
    public void invalid_missing_start_2() { }

    /** <p> text </p> </a> */
    public void invalid_missing_start_3() { }

    /** <img alt="image" src="image.png"> </img> */
    public void invalid_end() { }

    /** <invalid> </invalid> */
    public void unknown_start_end() { }

    /** <invalid> */
    public void unknown_start() { }

    /** </invalid> */
    public void unknown_end() { }
}

