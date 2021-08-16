/*
 * @test /nodynamiccopyright/
 * @bug 1265387 8048805
 * @summary ''' and '\u0027' are not legal char literals.
 * @author turnidge
 *
 * @compile/fail/ref=TripleQuote.out -XDrawDiagnostics  TripleQuote.java
 */

public
class TripleQuote {
    char c = '\u0027';
    char d = ''';
}
