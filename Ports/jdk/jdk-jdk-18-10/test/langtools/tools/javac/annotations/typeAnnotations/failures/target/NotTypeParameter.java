/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary test invalid location of TypeUse and TypeParameter
 * @author Mahmood Ali
 * @compile/fail/ref=NotTypeParameter.out -XDrawDiagnostics NotTypeParameter.java
 */

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

class VoidMethod<@A K> {
  @A void test() { }
}

@Target(ElementType.TYPE_USE)
@interface A { }

class TypeVariable<@B T> {
  @B T test1() { return null; }
  void test2(@B T p) {}
}

@Target(ElementType.TYPE_PARAMETER)
@interface B { }
