/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref InvalidEntity.out InvalidEntity.java
 */

// tidy: Warning: replacing invalid numeric character reference .*

// See
// http://www.w3.org/TR/html4/sgml/entities.html
// http://stackoverflow.com/questions/631406/what-is-the-difference-between-em-dash-151-and-8212

/**
 * &#01;
 * &#x01;
 * &splodge;
 *
 */
public class InvalidEntity { }
