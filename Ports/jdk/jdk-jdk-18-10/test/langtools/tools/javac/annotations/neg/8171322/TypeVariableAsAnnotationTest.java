/*
 * @test /nodynamiccopyright/
 * @bug 8171322
 * @summary AssertionError in TypeSymbol.getAnnotationTypeMetadata
 * @compile SimpleProcessor.java
 * @compile/fail/ref=TypeVariableAsAnnotationTest.out -processor SimpleProcessor -XDrawDiagnostics TypeVariableAsAnnotationTest.java
 */

class TypeVariableAsAnnotationTest<Override> {
  TypeVariableAsAnnotationTest(@Override String foo, @XXX String goo) {}
}