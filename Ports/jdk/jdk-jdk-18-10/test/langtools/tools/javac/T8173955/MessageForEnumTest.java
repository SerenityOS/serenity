/*
 * @test  /nodynamiccopyright/
 * @bug 8174027
 * @summary error message should adapt to the corresponding top level element
 * @compile/fail/ref=MessageForEnumTest.out -XDrawDiagnostics MessageForEnumTest.java
 */

public enum MessageForEnumTest_ {}
