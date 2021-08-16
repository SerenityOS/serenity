/*
 * @test /nodynamiccopyright/
 * @bug 8012722
 * @summary Single comma in array initializer should parse
 * @compile/fail/ref=SingleCommaAnnotationValueFail.out -XDrawDiagnostics
 * SingleCommaAnnotationValueFail.java
 */

public class SingleCommaAnnotationValueFail {
    // Non-example
    @Foo({,0}) void a() { }
}
@interface Foo { int[] value(); }
