/*
 * @test /nodynamiccopyright/
 * @test
 * @bug 6843077 8006775
 * @summary test old array syntax
 * @author Mahmood Ali
 * @compile/fail/ref=OldArray.out -XDrawDiagnostics OldArray.java
 */
import java.lang.annotation.*;

class OldArray {
  String [@A]  s() { return null; }
}

@Target(ElementType.TYPE_USE)
@interface A { }
