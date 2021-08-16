/*
 * @test /nodynamiccopyright/
 * @bug 8031383
 * @summary Verify that the parser handles a missing closing brace of a block gracefully.
 * @compile/fail/ref=MissingClosingBrace.out -XDrawDiagnostics MissingClosingBrace.java
 */

public class MissingClosingBrace {
    private void test(int i) {
        if (i > 0) {

    private int i;
}
