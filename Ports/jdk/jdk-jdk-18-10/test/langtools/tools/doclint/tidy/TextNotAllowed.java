/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247957
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref TextNotAllowed.out TextNotAllowed.java
 */

// tidy: Warning: plain text isn't allowed in <.*> elements

/**
 * <table> <caption> description </caption> abc </table>
 * <table> <caption> description </caption> <tbody> abc </tbody> </table>
 * <table> <caption> description </caption> <tr> abc </tr> </table>
 *
 * <dl> abc </dl>
 * <ol> abc </ol>
 * <ul> abc </ul>
 *
 * <ul>
 *     <li> item
 *     <li> item
 * </ul>
 */
public class TextNotAllowed { }
