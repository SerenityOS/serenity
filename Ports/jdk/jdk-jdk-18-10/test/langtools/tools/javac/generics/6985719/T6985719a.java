/*
 * @test /nodynamiccopyright/
 * @bug     6985719
 * @summary Alike methods in interfaces (Inheritance and Overriding)
 * @author  mcimadamore
 * @compile/fail/ref=T6985719a.out -XDrawDiagnostics T6985719a.java
 */

import java.util.List;

class T6985719a {
    interface A { void f(List<String> ls); }
    interface B { void f(List<Integer> ls); }
    interface C extends A,B {}
}
