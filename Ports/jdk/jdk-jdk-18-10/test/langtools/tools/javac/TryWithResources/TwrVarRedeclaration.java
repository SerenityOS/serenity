/*
 * @test /nodynamiccopyright/
 * @bug 7196163
 * @summary Variable redeclaration inside twr block
 * @compile/fail/ref=TwrVarRedeclaration.out -XDrawDiagnostics TwrVarRedeclaration.java
 */

public class TwrVarRedeclaration implements AutoCloseable {

    public static void main(String... args) {
        TwrVarRedeclaration r = new TwrVarRedeclaration();

        try (r) {
            TwrVarRedeclaration r = new TwrVarRedeclaration();
        }

        try (r) {
            Object r = new Object();
        }

        try (r) {
        } catch (Exception e) {
            Exception r = new Exception();
        }
    }

    public void close() {}
}
