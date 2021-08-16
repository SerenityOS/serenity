/*
 * @test /nodynamiccopyright/
 * @bug 8028389
 * @summary javac should output a proper error message when given something
 * like new Object(){} as annotation argument.
 *
 * @compile/fail/ref=AnonSubclass.out -XDrawDiagnostics AnonSubclass.java
 */

@AnonSubclass(new Object(){})
@interface AnonSubclass {
    String value();
}
