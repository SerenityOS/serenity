/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247957
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref AnchorAlreadyDefined.out AnchorAlreadyDefined.java
 */

// tidy: Warning: <.*> anchor ".*" already defined

/**
 * <a id="here">valid</a>
 * <a id="here">duplicate</a>
 * <h2 id="here">duplicate</h2>
 */
public class AnchorAlreadyDefined { }
