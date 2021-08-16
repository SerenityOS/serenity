/*
 * @test /nodynamiccopyright/
 * @bug 6979683
 * @summary Verify that casts can narrow and unbox at the same time
 * @author jrose
 *
 * @compile/fail/ref=TestCast6979683_BAD36.java.errlog -XDrawDiagnostics TestCast6979683_BAD36.java
 */

public class TestCast6979683_BAD36 {
    //...
    //...
    static int iconvBAD2(Comparable<Integer> o) { return o; } //BAD: cast needed
    //...
    //...
    //...
}
