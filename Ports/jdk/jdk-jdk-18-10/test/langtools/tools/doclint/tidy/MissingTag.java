/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref MissingTag.out MissingTag.java
 */

// tidy: Warning: missing <.*>
// tidy: Warning: missing </.*> before </.*>

/**
 * </p>
 * <h2> <b> text </h2>
 */
public class MissingTag { }
