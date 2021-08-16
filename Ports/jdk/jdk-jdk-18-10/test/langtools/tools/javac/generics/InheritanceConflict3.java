/*
 * @test /nodynamiccopyright/
 * @bug 4984158
 * @summary two inherited methods with same signature
 * @author darcy
 *
 * @compile/fail/ref=InheritanceConflict3.out -XDrawDiagnostics  InheritanceConflict3.java
 */

package inheritance.conflict3;

class X1<T> {
    int f(T t) { throw null; }
    void f(Object o) {}
}

class X2 extends X1 {
}
