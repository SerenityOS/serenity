/*
 * @test /nodynamiccopyright/
 * @bug 8210483
 * @summary AssertionError in DeferredAttr at setOverloadKind caused by JDK-8203679
 * @compile/fail/ref=MethodRefStuck.out -XDrawDiagnostics MethodRefStuck.java
 */

import java.util.Optional;
import java.util.stream.Stream;

public abstract class MethodRefStuck {
  public static void main(Stream<String> xs, Optional<String> x) {
    xs.map(
        c -> {
          return new I(x.map(c::equals));
        });
  }

  static class I {
    I(boolean i) {}
  }
}
