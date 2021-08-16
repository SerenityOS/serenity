/*
 * @test /nodynamiccopyright/
 * @bug 6979683
 * @summary Verify that casts can narrow and unbox at the same time
 * @author jrose
 *
 * @compile/fail/ref=TestCast6979683_BAD38.java.errlog -XDrawDiagnostics TestCast6979683_BAD38.java
 */

public class TestCast6979683_BAD38 {
    //...
    //...
    //...
    //...
    static float cconvBAD1(Comparable<Character> o) { return o; } //BAD
    //...
}
