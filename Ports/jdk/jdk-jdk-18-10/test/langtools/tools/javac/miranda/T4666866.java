/*
 * @test  /nodynamiccopyright/
 * @bug 4666866 4785453
 * @summary REGRESSION: Generated error message unhelpful for missing methods
 * @author gafter
 *
 * @compile/fail/ref=T4666866.out -XDrawDiagnostics T4666866.java
 */

class t implements Runnable {}
