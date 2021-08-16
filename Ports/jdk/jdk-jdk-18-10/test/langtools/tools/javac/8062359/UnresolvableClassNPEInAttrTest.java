/*
 * @test /nodynamiccopyright/
 * @bug 8062359
 * @summary NullPointerException in Attr when type-annotating an anonymous
 *          inner class in an unresolvable class
 * @compile/fail/ref=UnresolvableClassNPEInAttrTest.out -XDrawDiagnostics UnresolvableClassNPEInAttrTest.java
 */

public class UnresolvableClassNPEInAttrTest {
    public static void main(String[] args) {
        new Undefined() {
            void test() {
                new Object() {};
            }
        };
    }
}
