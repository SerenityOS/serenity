/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247957
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref InvalidName.out InvalidName.java
 */

// tidy: Warning: <a> cannot copy name attribute to id

/**
 * <a id="abc">valid</a>
 * <a id="abc123">valid</a>
 * <a id="a.1:2-3_4">valid</a>
 * <a id="foo()">valid</a>
 * <a id="foo() ">invalid</a>
 */
public class InvalidName { }
