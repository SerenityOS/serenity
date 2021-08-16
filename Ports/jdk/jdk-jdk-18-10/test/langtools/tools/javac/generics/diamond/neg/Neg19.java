/*
 * @test /nodynamiccopyright/
 * @bug 8062373
 * @summary Test that when inaccessible types constitute the inferred types of <> the compiler complains.
 * @compile/fail/ref=Neg19.out Neg19.java -XDrawDiagnostics
 */



class Neg19 {
    public static void main(String[] args) {
        new Neg19_01<Neg19>().foo(new Neg19_01<>()); // OK.
        new Neg19_01<Neg19>().foo(new Neg19_01<>() {}); // ERROR.
    }
}

class Neg19_01<T> {
    private class Private {}
    Neg19_01() {}
    void foo(Neg19_01<Private> p) {}
}
