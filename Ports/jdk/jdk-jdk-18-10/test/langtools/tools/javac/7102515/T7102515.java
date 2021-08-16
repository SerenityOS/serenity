/*
 * @test /nodynamiccopyright/
 * @bug 7102515
 * @summary javac running very very long and not returning
 * @compile/fail/ref=T7102515.out -XDrawDiagnostics T7102515.java
 */

class T7102515 {
    T7102515 badBinary = new T7102515() + new T7102515();
    Object badUnary = badBinary++;
}
