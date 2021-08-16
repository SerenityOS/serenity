/*
 * @test /nodynamiccopyright/
 * @bug     6946618 6968497
 * @summary sqe test fails: javac/generics/NewOnTypeParm  in pit jdk7 b91 in all platforms.
 * @author  mcimadamore
 * @compile/fail/ref=T6946618c.out -XDrawDiagnostics T6946618c.java
 */

class T6946618c {
    static class C<T> { }

    void test() {
        C<?> c1 = new C<? extends String>();
        C<?> c2 = new C<? super String>();
        C<?> c3 = new C<?>();
    }
}
