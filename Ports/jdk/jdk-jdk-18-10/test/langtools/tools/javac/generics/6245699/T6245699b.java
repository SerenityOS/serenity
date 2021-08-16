/*
 * @test    /nodynamiccopyright/
 * @bug     6270396 6245699
 * @summary Missing bridge for final method (gives AbstractMethodError at runtime)
 * @compile/fail/ref=T6245699b.out -XDrawDiagnostics  T6245699b.java
 */

public class T6245699b {
    public static void main(String[] args) {
        IBar b = new Bar();
        String x = b.doIt();
    }

    static class Foo<T> {
        public final T doIt() { return null; }
    }

    static interface IBar {
        String doIt();
    }

    static class Bar extends Foo<String> implements IBar {
        public String doIt() { // assert that a final method can't be overridden
            return null;
        }
    }
}
