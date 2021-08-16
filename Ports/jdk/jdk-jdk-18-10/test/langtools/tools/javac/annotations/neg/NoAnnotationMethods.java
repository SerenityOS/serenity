/*
 * @test    /nodynamiccopyright/
 * @bug     6393539
 * @summary no compile-time error for clone, etc. in annotation type
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=NoAnnotationMethods.out -XDrawDiagnostics  NoAnnotationMethods.java
 */

public @interface NoAnnotationMethods {
    int annotationType();
}
