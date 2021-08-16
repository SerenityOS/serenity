/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary test indexing of an array
 * @author Mahmood Ali
 * @compile/fail/ref=IndexArray.out -XDrawDiagnostics IndexArray.java
 */
import java.lang.annotation.*;

class IndexArray {
  int[] var;
  int a = var @A [1];
}

@Target(ElementType.TYPE_USE)
@interface A { }
