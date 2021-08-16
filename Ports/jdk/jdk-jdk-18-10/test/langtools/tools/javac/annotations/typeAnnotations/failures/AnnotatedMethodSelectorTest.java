/*
 * @test /nodynamiccopyright/
 * @bug 8145987
 * @summary Assertion failure when compiling stream with type annotation
 * @compile/fail/ref=AnnotatedMethodSelectorTest.out -XDrawDiagnostics AnnotatedMethodSelectorTest.java
 */


class AnnotatedMethodSelectorTest {
    @interface A {}
    static public void main(String... args) {
        java.util.@A Arrays.stream(args);
    }
}
