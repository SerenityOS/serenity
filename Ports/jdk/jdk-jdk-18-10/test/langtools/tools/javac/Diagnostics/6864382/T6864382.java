/**
 * @test /nodynamiccopyright/
 * @bug     6864382
 * @summary NullPointerException when compiling a negative java source
 * @author  mcimadamore
 * @compile/fail/ref=T6864382.out -XDrawDiagnostics  T6864382.java
 */

class T6864382<T> extends T {}
