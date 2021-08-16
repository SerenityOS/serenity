/*
 * @test /nodynamiccopyright/
 * @bug 8200199
 * @summary javac suggests to use var even when var is used
 * @compile/fail/ref=T8200199.out -Werror -XDfind=local -XDrawDiagnostics T8200199.java
 */

class T8200199 {

    class Resource implements AutoCloseable {
        public void close() {};
    }

    public void implicit() {
        var i = 33;
        for (var x = 0 ; x < 10 ; x++) { }
        try (var r = new Resource()) { }
    }

    public void explicit() {
        int i = 33;
        for (int x = 0 ; x < 10 ; x++) { }
        try (Resource r = new Resource()) { }
    }
}
