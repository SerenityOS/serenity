/*
 * @test /nodynamiccopyright/
 * @bug 7175433 6313164
 * @summary Inference cleanup: add helper class to handle inference variables
 * @compile/fail/ref=T7175433.out -XDrawDiagnostics T7175433.java
 */

import java.util.List;

class Bar {

    private class Foo { }

    <Z> List<Z> m(Object... o) { return null; }
    <Z> List<Z> m(Foo... o) { return null; }

    Foo getFoo() { return null; }
}

public class T7175433 {

    public static void main(String[] args) {
        Bar b = new Bar();
        b.m(b.getFoo());
    }
}
