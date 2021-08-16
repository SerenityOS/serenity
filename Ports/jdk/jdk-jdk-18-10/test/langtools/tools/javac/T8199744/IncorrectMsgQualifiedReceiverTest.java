/*
 * @test  /nodynamiccopyright/
 * @bug 8199744
 * @summary Incorrect compiler message for ReceiverParameter in inner class constructor
 * @compile/fail/ref=IncorrectMsgQualifiedReceiverTest.out -XDrawDiagnostics IncorrectMsgQualifiedReceiverTest.java
 */

class IncorrectMsgQualifiedReceiverTest {
    void foo(int any, IncorrectMsgQualifiedReceiverTest IncorrectMsgQualifiedReceiverTest.this) {}
    void bar(int any, IncorrectMsgQualifiedReceiverTest IncorrectMsgQualifiedReceiverTest.this, int another) {}
}
