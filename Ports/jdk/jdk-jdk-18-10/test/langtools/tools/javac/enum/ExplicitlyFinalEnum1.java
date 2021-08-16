/*
 * @test /nodynamiccopyright/
 * @bug 5009601
 * @summary enum's cannot be explicitly declared final even if they are
 * @author Joseph D. Darcy
 * @compile/fail/ref=ExplicitlyFinalEnum1.out -XDrawDiagnostics ExplicitlyFinalEnum1.java
 */

final enum ExplicitlyFinalEnum1 {
    FE,
    FI,
    FO,
    FUM;
}
