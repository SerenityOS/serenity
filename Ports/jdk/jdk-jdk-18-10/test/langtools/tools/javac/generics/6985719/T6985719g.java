/*
 * @test /nodynamiccopyright/
 * @bug     6985719 7170058
 * @summary Alike methods in interfaces (Inheritance and Overriding)
 * @author  mcimadamore
 * @compile/fail/ref=T6985719g.out -XDrawDiagnostics T6985719g.java
 */

import java.util.List;

class T6985719g {
    interface A<X> { void f(List<X> ls); }
    interface B extends A<String> { void f(List<Integer> ls); }
}
