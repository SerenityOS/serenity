/*
 * @test /nodynamiccopyright/
 * @bug 4738171
 * @summary generics: problem with equivalence of generic types
 * @author gafter
 *
 * @compile/fail/ref=T4738171.out -XDrawDiagnostics   T4738171.java
 */

interface If<T> {
    final If<T> C0 = null;
}
