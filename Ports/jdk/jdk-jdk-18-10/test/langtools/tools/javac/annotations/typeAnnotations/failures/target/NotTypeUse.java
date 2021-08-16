/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary test invalid location of TypeUse
 * @author Mahmood Ali
 * @compile/fail/ref=NotTypeUse.out -XDrawDiagnostics NotTypeUse.java
 */

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

class VoidMethod {
  @A void test() { }
}

@Target(ElementType.TYPE)
@interface A { }
