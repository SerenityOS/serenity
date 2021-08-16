/*
 * @test /nodynamiccopyright/
 * @bug 7042623
 * @summary Regression: javac silently crash when attributing non-existent annotation
 * @compile/fail/ref=T7042623.out -XDrawDiagnostics -XDdev T7042623.java
 */

@interface Defined2 {}

@Undefined1(@Defined2)
class Test1{}

