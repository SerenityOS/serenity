/*
 * @test /nodynamiccopyright/
 * @bug     6985719 7170058
 * @summary Alike methods in interfaces (Inheritance and Overriding)
 * @author  mcimadamore
 * @compile/fail/ref=T6985719d.out -XDrawDiagnostics T6985719d.java
 */

import java.util.List;

class T6985719d {
    abstract class A { abstract void f(List<String> ls); }
    interface B<X> { void f(List<X> ls); }
    abstract class C extends A implements B<Integer> {}
}
