/*
 * @test /nodynamiccopyright/
 * @bug 6450290
 * @summary Capture of nested wildcards causes type error
 * @author Maurizio Cimadamore
 * @compile/fail/ref=T6450290.out -XDrawDiagnostics  T6450290.java
 */

public class T6450290 {
    static class Box<X extends Box<?,?>, T extends X> {
        T value;
        Box<X, T> same;
    }

    static class A extends Box<A,A> {}
    static class B extends Box<B,B> {}
    public static void main(String[] args) {
        Box<?,?> b = new Box<Box<A,A>,Box<A,A>>();
        b.value.same = new Box<B,B>(); //javac misses this bad assignment
    }
}
