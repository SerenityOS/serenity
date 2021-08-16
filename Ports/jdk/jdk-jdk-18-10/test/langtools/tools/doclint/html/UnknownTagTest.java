/*
 * @test /nodynamiccopyright/
 * @bug 8264843
 * @summary Javac crashes with NullPointerException when finding unencoded XML in <pre> tag
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs -ref UnknownTagTest.out UnknownTagTest.java
 */

/**
 * This is an <unknown> tag.
 * This is an <unknown a=b> tag with attributes.
 * This is an <unknown/> self-closing tag.
 */
public class UnknownTagTest {
}
