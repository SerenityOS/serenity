/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary check for missing annotation value
 * @author Mahmood Ali
 * @compile/fail/ref=MissingAnnotationValue.out -XDrawDiagnostics MissingAnnotationValue.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

class MissingAnnotationValue {
  void test() {
    new @A String();
  }
}

@Target(ElementType.TYPE_USE)
@interface A { int field(); }
