/*
 * @test  /nodynamiccopyright/
 * @bug 6911256 6964740
 * @author Joseph D. Darcy
 * @summary Verify bad TWRs don't compile
 * @compile/fail/ref=BadTwr.out -XDrawDiagnostics BadTwr.java
 */

public class BadTwr implements AutoCloseable {
    public static void main(String... args) {
        // illegal repeated name
        try(BadTwr r1 = new BadTwr(); BadTwr r1 = new BadTwr()) {
            System.out.println(r1.toString());
        }

        // illegal duplicate name of method argument
        try(BadTwr args = new BadTwr()) {
            System.out.println(args.toString());
            final BadTwr thatsIt = new BadTwr();
            thatsIt = null;
        }

        try(BadTwr name = new BadTwr()) {
            // illegal duplicate name of enclosing try
            try(BadTwr name = new BadTwr()) {
                System.out.println(name.toString());
            }
        }

    }

    public void close() {
        ;
    }
}
