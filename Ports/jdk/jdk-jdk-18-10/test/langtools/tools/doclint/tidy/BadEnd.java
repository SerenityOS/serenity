/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247957
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref BadEnd.out BadEnd.java
 */

// tidy: Warning: <.*> is probably intended as </.*>

/**
 * <a id="here"> text <a>
 * <code> text <code>
 */
public class BadEnd { }
