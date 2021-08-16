/*
 * @test    /nodynamiccopyright/
 * @bug     6209839
 * @summary Illegal forward reference to enum constants allowed by javac
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=TestEnum5.out -XDrawDiagnostics  TestEnum5.java
 */

enum TestEnum {
    BAR,
    QUX,
    BAZ;
    static String X = "X";
    TestEnum() {
        String y = X;
    }
}
