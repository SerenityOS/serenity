/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247957
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref TagNotAllowed.out TagNotAllowed.java
 */

/**
 * <dl> <b>abc</b> <dt> term </dt> <b>def</b> <dd> description </dd> <b>ghi</b> </dl>
 * <ol> <b>abc</b> <li> item </li> <b>def</b> <li> item </li> <b>ghi</b> </ol>
 * <ul> <b>abc</b> <li> item </li> <b>def</b> <li> item </li> <b>ghi</b> </ul>
 *
 * <table summary=description> <b>abc</b> </table>
 * <table summary=description> <thead> <b>abc</b> </thead> </table>
 * <table summary=description> <tbody> <b>abc</b> </tbody> </table>
 * <table summary=description> <tfoot> <b>abc</b> </tfoot> </table>
 * <table summary=description> <tr> <b>abc</b> </tr> </table>
 *
 * <pre>
 *   <img alt="image" src="image.png">
 *   <p> para </p>
 *   <big> text </big>
 *   <small> text </small>
 *   <sub> text </sub>
 *   <sup> text </sup>
 * </pre>
 */
public class TagNotAllowed { }
