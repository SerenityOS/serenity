/*
 * @test /nodynamiccopyright/
 * @bug 6594284
 * @summary NPE thrown when calling a method on an intersection type
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T6594284.out -XDrawDiagnostics  T6594284.java
 */

public class T6594284 {
    class A{ public void a(){}}
    class B extends A{ public void b(){}}
    interface I{ void i();}
    interface I1 { void i1(); }
    class E extends B implements I{ public void i(){};}

    class C<W extends B & I1, T extends W>{
        C<? extends I, ? extends E> arg;
    }
}
