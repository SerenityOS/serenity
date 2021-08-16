/*
 * @test    /nodynamiccopyright/
 * @bug     6407257
 * @summary javac locks up when encountering cyclic inheritance
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T6407257.out -XDrawDiagnostics T6407257.java
 */

class T6407257a extends T6407257a {}

public class T6407257 extends T6407257a {
    public static void main(String... args) {
        main(args);
    }
}
