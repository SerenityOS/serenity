/*
 * @test    /nodynamiccopyright/
 * @bug     6209839
 * @summary Illegal forward reference to enum constants allowed by javac
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=TestEnum4.out -XDrawDiagnostics  TestEnum4.java
 */

enum TestEnum {
    BAR,
    QUX,
    BAZ;
    static String X = "X";
    private String y = X;
}
