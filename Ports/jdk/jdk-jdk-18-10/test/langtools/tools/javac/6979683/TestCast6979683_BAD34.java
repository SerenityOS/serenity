/*
 * @test /nodynamiccopyright/
 * @bug 6979683
 * @summary Verify that casts can narrow and unbox at the same time
 * @author jrose
 *
 * @compile/fail/ref=TestCast6979683_BAD34.java.errlog -XDrawDiagnostics TestCast6979683_BAD34.java
 */

public class TestCast6979683_BAD34 {
    static boolean zconvBAD1(Number o) { return o; } //BAD
    //...
    //...
    //...
    //...
    //...
}
