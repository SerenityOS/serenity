/*
 * @test /nodynamiccopyright/
 * @bug 6979683
 * @summary Verify that casts can narrow and unbox at the same time
 * @author jrose
 *
 * @compile/fail/ref=TestCast6979683_BAD37.java.errlog -XDrawDiagnostics TestCast6979683_BAD37.java
 */

public class TestCast6979683_BAD37 {
    //...
    //...
    //...
    static int iconvBAD3(Comparable<Short> o) { return (int)o; } //BAD: wrong instance
    //...
    //...
}
