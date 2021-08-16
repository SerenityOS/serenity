/*
 * @test /nodynamiccopyright/
 * @bug     6985719 7170058
 * @summary Alike methods in interfaces (Inheritance and Overriding)
 * @author  mcimadamore
 * @compile/fail/ref=T6985719c.out -XDrawDiagnostics T6985719c.java
 */

import java.util.List;

class T6985719c {
    interface A { void f(List<String> ls); }
    interface B<X> { void f(List<X> ls); }
    interface C extends A,B<Integer> {}
}
