/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary check for invalid annotatins given the target
 * @author Mahmood Ali
 * @compile/fail/ref=InvalidLocation.out -XDrawDiagnostics InvalidLocation.java
 */

class InvalidLocation<K> {
  InvalidLocation<@A String> l;
}

@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
@interface A { }
