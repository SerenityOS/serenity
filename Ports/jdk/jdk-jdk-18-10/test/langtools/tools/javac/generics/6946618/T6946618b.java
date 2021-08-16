/*
 * @test /nodynamiccopyright/
 * @bug     6946618
 * @summary sqe test fails: javac/generics/NewOnTypeParm  in pit jdk7 b91 in all platforms.
 * @author  mcimadamore
 * @compile/fail/ref=T6946618b.out -XDrawDiagnostics T6946618b.java
 */

class T6946618b {
    static class C<T> {
      T makeT() {
        return new T<>(); //error
      }
    }

    static class D<S> {
      C<S> makeC() {
        return new C<>(); //ok
      }
    }
}
