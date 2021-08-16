/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   Test that lambda conversion is only for SAM interface, not abstract class
 * @compile/fail/ref=AbstractClass_neg.out -XDrawDiagnostics AbstractClass_neg.java
 */

public class AbstractClass_neg {

    abstract class SAM {
        abstract int m();
    }

    void test() {
        SAM s = ()-> 6;
    }
}
