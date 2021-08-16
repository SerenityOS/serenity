/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary check for Duplicate annotation value
 * @author Mahmood Ali
 * @compile/fail/ref=DuplicateAnnotationValue.out -XDrawDiagnostics DuplicateAnnotationValue.java
 */
import java.lang.annotation.*;
class DuplicateAnnotationValue {
  void test() {
    new @A String();
  }
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { int field(); }
