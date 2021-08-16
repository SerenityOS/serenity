/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref ParaInPre.out ParaInPre.java
 */

// tidy: Warning: replacing <p> by <br>
// tidy: Warning: using <br> in place of <p>

/**
 * <pre>
 *     text
 *     <p>
 *     more text
 * </pre>
 */
public class ParaInPre { }
