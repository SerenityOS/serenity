/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref NestedTag.out NestedTag.java
 */

// tidy: Warning: nested emphasis <.*>

/**
 * <b><b> text </b></b>
 * {@link java.lang.String <code>String</code>}
 */
public class NestedTag { }
