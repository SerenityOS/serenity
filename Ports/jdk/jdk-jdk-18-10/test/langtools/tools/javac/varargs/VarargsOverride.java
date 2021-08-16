/*
 * @test /nodynamiccopyright/
 * @bug 4984022
 * @summary overriding with method of different arity is prohibited
 * @author gafter
 *
 * @compile  VarargsOverride.java
 * @compile/ref=VarargsOverride.out -XDrawDiagnostics -Xlint VarargsOverride.java
 */

package varargs.override;

class A {
    void f(Object[] o) {}
}

class B extends A {
    void f(Object... o) {}
}
