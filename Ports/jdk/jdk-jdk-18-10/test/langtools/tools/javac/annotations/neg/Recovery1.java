/*
 * @test /nodynamiccopyright/
 * @bug 4963450
 * @summary Assertion error is thrown when an annotation class cannot be found.
 * @author gafter
 *
 * @compile/fail/ref=Recovery1.out -XDrawDiagnostics  Recovery1.java
 */

package recovery1;

@interface MyAnnotation {
    String value();
    Marker marker() default @Marker;
}

interface MyBean {
    @MyAnnotation (value="baz", markerToo="not default")
    public String getFoo();
}
