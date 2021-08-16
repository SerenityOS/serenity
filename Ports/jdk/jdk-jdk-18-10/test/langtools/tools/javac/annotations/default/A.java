/*
 * @test /nodynamiccopyright/
 * @bug 4901262
 * @summary Constraints regarding annotation defaults
 * @author gafter
 *
 * @compile A.java B.java C.java
 * @compile/fail/ref=Derr.out -XDrawDiagnostics  Derr.java
 * @compile/fail/ref=Eerr.out -XDrawDiagnostics  Eerr.java
 */

public @interface A {
    int x();
    int y() default 3;
}
