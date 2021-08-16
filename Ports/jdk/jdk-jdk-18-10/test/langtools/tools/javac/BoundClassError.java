/*
 * @test /nodynamiccopyright/
 * @bug 4980352
 * @summary Verify compiler doesn't throw a NullPointerException when compiling.
 * @author tball
 *
 * @compile/fail/ref=BoundClassError.out -XDrawDiagnostics  BoundClassError.java
 */
public class BoundClassError <T extends String&Comparable<BoundClassError>> {}
