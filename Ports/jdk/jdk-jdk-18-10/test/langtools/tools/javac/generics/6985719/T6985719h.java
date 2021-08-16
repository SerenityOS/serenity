/*
 * @test /nodynamiccopyright/
 * @bug     6985719 7170058
 * @summary Alike methods in interfaces (Inheritance and Overriding)
 * @author  mcimadamore
 * @compile/fail/ref=T6985719h.out -XDrawDiagnostics T6985719h.java
 */

import java.util.List;

class T6985719h {
    abstract class A<X> { abstract void f(List<X> ls); }
    abstract class B extends A<String> { abstract void f(List<Integer> ls); }
}
