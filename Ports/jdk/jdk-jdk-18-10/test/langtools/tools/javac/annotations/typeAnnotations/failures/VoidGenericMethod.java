/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary test type annotation on void generic methods
 * @author Mahmood Ali
 * @compile/fail/ref=VoidGenericMethod.out -XDrawDiagnostics  VoidGenericMethod.java
 */

import java.lang.annotation.*;
class VoidGenericMethod {
  public @A <T> void method() { }
}

@Target(ElementType.TYPE_USE)
@interface A { }
