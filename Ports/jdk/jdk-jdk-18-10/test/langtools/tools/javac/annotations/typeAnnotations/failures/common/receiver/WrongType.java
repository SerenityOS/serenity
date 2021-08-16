/*
 * @test /nodynamiccopyright/
 * @bug 8006775
 * @summary the receiver parameter has the type of the surrounding class
 * @author Werner Dietl
 * @compile/fail/ref=WrongType.out -XDrawDiagnostics WrongType.java
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
@interface A {}

class WrongType {
  Object f;

  void good1(@A WrongType this) {}

  void good2(@A WrongType this) {
    this.f = null;
    Object o = this.f;
  }

  void bad1(@A Object this) {}

  void bad2(@A Object this) {
    this.f = null;
    Object o = this.f;
  }

  void wow(@A XYZ this) {
    this.f = null;
  }

  class Inner {
    void good1(@A Inner this) {}
    void good2(@A WrongType.Inner this) {}

    void outerOnly(@A WrongType this) {}
    void wrongInner(@A Object this) {}
    void badOuter(@A Outer.Inner this) {}
    void badInner(@A WrongType.XY this) {}
  }

  class Generics<X> {
    <Y> void m(Generics<Y> this) {}
  }
}
