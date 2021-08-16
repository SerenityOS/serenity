/*
 * @test /nodynamiccopyright/
 * @bug 8193142
 * @summary Regression: ClassCastException: Type$ErrorType cannot be cast to Type$ArrayType
 * @compile/fail/ref=ElementTypeMissingTest.out -XDrawDiagnostics -XDdev ElementTypeMissingTest.java
 */

public class ElementTypeMissingTest { void m(Unkn... own) { } }