/*
 * @test /nodynamiccopyright/
 * @bug 5014305
 * @summary Malformed annotation type with varargs parameter crashes javac
 *
 * @compile/fail/ref=BadAnnotation.out -XDrawDiagnostics  BadAnnotation.java
 */

   @BadAnnotation(1)
   @interface BadAnnotation {
           int value(int... illegal);
   }
