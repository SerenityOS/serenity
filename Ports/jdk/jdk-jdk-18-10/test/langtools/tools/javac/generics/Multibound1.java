/*
 * @test /nodynamiccopyright/
 * @bug 4482403
 * @summary javac failed to check second bound
 * @author gafter
 *
 * @compile/fail/ref=Multibound1.out -XDrawDiagnostics  Multibound1.java
 */

package Multibound1;

interface A {}
interface B {}
class C<T extends A&B> {}
class D implements A {}
class E extends C<D> {}
