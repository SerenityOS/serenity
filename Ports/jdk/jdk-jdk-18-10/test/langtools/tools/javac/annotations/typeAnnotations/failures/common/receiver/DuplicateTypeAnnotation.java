/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary check for duplicate annotations in receiver
 * @author Mahmood Ali
 * @compile/fail/ref=DuplicateTypeAnnotation.out -XDrawDiagnostics DuplicateTypeAnnotation.java
 */
import java.lang.annotation.*;
class DuplicateTypeAnnotation {
  void test(@A @A DuplicateTypeAnnotation this) { }
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { }
