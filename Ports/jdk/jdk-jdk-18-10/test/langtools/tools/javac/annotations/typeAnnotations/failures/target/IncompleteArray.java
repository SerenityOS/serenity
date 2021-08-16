/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary test incomplete array declaration
 * @author Mahmood Ali
 * @compile/fail/ref=IncompleteArray.out -XDrawDiagnostics IncompleteArray.java
 */
class IncompleteArray {
  int @A [] @A var;
}

@interface A { }
