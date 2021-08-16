/*
 * @test /nodynamiccopyright/
 * @bug 4984158
 * @summary two inherited methods with same signature
 * @author gafter, Maurizio Cimadamore
 *
 * @compile/fail/ref=InheritanceConflict.out -XDrawDiagnostics   InheritanceConflict.java
 */

package inheritance.conflict;

class A<T> {
    void f(String s) {}
}

class B<T> extends A<T> {
    void f(T t) {}
}

class C extends B<String> {
}
