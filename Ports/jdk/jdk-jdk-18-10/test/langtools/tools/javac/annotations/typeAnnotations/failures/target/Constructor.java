/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary test invalid location of TypeUse
 * @author Mahmood Ali
 * @compile/fail/ref=Constructor.out -XDrawDiagnostics Constructor.java
 */

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

class Constructor {
  // Constructor result type use annotation
  @A Constructor() { }

  // Not type parameter annotation
  @B Constructor(int x) { }

  // TODO add err: no "this" receiver parameter for constructors
  // Constructor(@A Constructor this, Object o) { }

  // TODO: support Outer.this.
}

class Constructor2 {
  class Inner {
    // OK
    @A Inner() { }
  }
}

@Target(ElementType.TYPE_USE)
@interface A { }

@Target(ElementType.TYPE_PARAMETER)
@interface B { }

