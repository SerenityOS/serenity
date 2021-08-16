/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary check for missing annotation value
 * @author Mahmood Ali
 * @compile/fail/ref=MissingAnnotationValue.out -XDrawDiagnostics MissingAnnotationValue.java
 */
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

class MissingAnnotationValue {
  void test(@A MissingAnnotationValue this) { }
}

@Target({TYPE_USE})
@interface A { int field(); }
