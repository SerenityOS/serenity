/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref UnescapedOrUnknownEntity.out UnescapedOrUnknownEntity.java
 */

// tidy: Warning: unescaped & or unknown entity ".*"
// tidy: Warning: unescaped & which should be written as &amp;
// tidy: Warning: entity ".*" doesn't end in ';'

/**
 * L&F
 * Drag&Drop
 * if (a & b);
 */
public class UnescapedOrUnknownEntity { }
