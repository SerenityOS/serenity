/*
 * @test /nodynamiccopyright/
 * @bug 6979683
 * @summary Verify that casts can narrow and unbox at the same time
 * @author jrose
 *
 * @compile/fail/ref=TestCast6979683_BAD39.java.errlog -XDrawDiagnostics TestCast6979683_BAD39.java
 */

public class TestCast6979683_BAD39 {
    //...
    //...
    //...
    //...
    //...
    static float cconvBAD2(Number o) { return (char)o; } //BAD
}
