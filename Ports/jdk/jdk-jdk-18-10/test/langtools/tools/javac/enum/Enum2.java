/*
 * @test /nodynamiccopyright/
 * @summary enums: an enumeration type may not be extended
 * @author gafter
 * @compile/fail/ref=Enum2.out -XDrawDiagnostics  Enum2.java
 */

public class Enum2 {
    enum e1 { red, green, blue }
    static class e2 extends e1 {}
}
