/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary static field access isn't a valid location
 * @author Mahmood Ali
 * @compile/fail/ref=StaticFields.out -XDrawDiagnostics StaticFields.java
 */
import java.lang.annotation.*;

class C {
  static int f;
  // static block
  static {
    @A C.f = 1;
  }
  // static ref
  int a = @A C.f;
  // static method
  static int f() { return @A C.f; }
  // main
  public static void main(String... args) {
    int a = @A C.f;
  }
}

@Target(ElementType.TYPE_USE)
@interface A { }
