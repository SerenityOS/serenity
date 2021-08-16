/*
 * @test /nodynamiccopyright/
 * @bug 5016879
 * @summary REGRESSION: translation unit ending in identifier crashes javac
 * @author gafter
 *
 * @compile/fail/ref=EOI.out -XDrawDiagnostics EOI.java
 */

class foobar {}
foobar
