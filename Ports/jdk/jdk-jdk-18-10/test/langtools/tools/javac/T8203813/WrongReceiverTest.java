/*
 * @test  /nodynamiccopyright/
 * @bug 8203813
 * @summary javac accepts an illegal name as a receiver parameter name
 * @compile/fail/ref=WrongReceiverTest.out -XDrawDiagnostics WrongReceiverTest.java
 */

public class WrongReceiverTest {
    WrongReceiverTest wr;
    void f(WrongReceiverTest wr.wr) {}
}
