/*
 * @test /nodynamiccopyright/
 * @bug     6985719
 * @summary Alike methods in interfaces (Inheritance and Overriding)
 * @author  mcimadamore
 * @compile/fail/ref=T6985719e.out -XDrawDiagnostics T6985719e.java
 */

import java.util.List;

class T6985719e {
    interface A { void f(List<String> ls); }
    interface B extends A { void f(List<Integer> ls); }
}
