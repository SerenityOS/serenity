/**
 * @test  /nodynamiccopyright/
 * @bug 7104201
 * @summary Refactor DocCommentScanner
 * @compile/fail/ref=DeprecatedDocComment4.out -XDrawDiagnostics -Werror -Xlint:dep-ann DeprecatedDocComment4.java
 */

class DeprecatedDocComment4 {
    /** @deprecated **/
    /* block */
    void test1() {};

    /** @deprecated **/
    /** double javadoc */
    void test2() {};

    /** @deprecated **/
    //line comment
    void test3() {};
}
