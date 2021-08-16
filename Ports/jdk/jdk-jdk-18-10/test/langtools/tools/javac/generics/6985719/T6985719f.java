/*
 * @test /nodynamiccopyright/
 * @bug     6985719
 * @summary Alike methods in interfaces (Inheritance and Overriding)
 * @author  mcimadamore
 * @compile/fail/ref=T6985719f.out -XDrawDiagnostics T6985719f.java
 */

import java.util.List;

class T6985719f {
    abstract class A { abstract void f(List<String> ls); }
    abstract class B extends A { void f(List<Integer> ls); }
}
