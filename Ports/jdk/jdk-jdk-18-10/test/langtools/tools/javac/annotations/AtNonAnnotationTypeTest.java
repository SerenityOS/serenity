/*
 * @test /nodynamiccopyright/
 * @bug 8206325
 * @summary AssertionError in TypeSymbol.getAnnotationTypeMetadata
 * @compile/fail/ref=AtNonAnnotationTypeTest.out -XDrawDiagnostics -XDdev AtNonAnnotationTypeTest.java
 */

import java.lang.annotation.Annotation;
class AtNonAnnotationTypeTest<Override extends Annotation> {
  AtNonAnnotationTypeTest(@Override String foo) {}
}