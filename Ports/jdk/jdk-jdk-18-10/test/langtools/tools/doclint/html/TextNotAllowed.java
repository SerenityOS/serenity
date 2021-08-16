/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247957
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref TextNotAllowed.out TextNotAllowed.java
 */

/**
 * <dl> abc <dt> term </dt> def <dd> description </dd> ghi </dl>
 * <ol> abc <li> item </li> def <li> item </li> ghi </ol>
 * <ul> abc <li> item </li> def <li> item </li> ghi </ul>
 *
 * <table> <caption> description </caption> abc </table>
 * <table> <caption> description </caption> <thead> abc </thead> </table>
 * <table> <caption> description </caption> <tbody> abc </tbody> </table>
 * <table> <caption> description </caption> <tfoot> abc </tfoot> </table>
 * <table> <caption> description </caption> <tr> abc </tr> </table>
 *
 * <dl> &amp; <dt> term </dt> &lt; <dd> description </dd> &gt; </dl>
 * <ol> &amp; <li> item </li> &lt; <li> item </li> &gt; </ol>
 * <ul> &amp; <li> item </li> &lt; <li> item </li> &gt; </ul>
 *
 * <table> <caption> description </caption> &amp; </table>
 * <table> <caption> description </caption> <thead> &amp; </thead> </table>
 * <table> <caption> description </caption> <tbody> &amp; </tbody> </table>
 * <table> <caption> description </caption> <tfoot> &amp; </tfoot> </table>
 * <table> <caption> description </caption> <tr> &amp; </tr> </table>
 *
 */
public class TextNotAllowed { }
