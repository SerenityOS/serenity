/*
 * @test  /nodynamiccopyright/
 * @bug 8023545
 * @summary Misleading error message when using diamond operator with private constructor
 * @compile/fail/ref=MisleadingErrorMsgDiamondPlusPrivateCtorTest.out -XDrawDiagnostics MisleadingErrorMsgDiamondPlusPrivateCtorTest.java
 */

public class MisleadingErrorMsgDiamondPlusPrivateCtorTest {
    public void foo() {
        MyClass<Object> foo = new MyClass<>();
    }
}

class MyClass<E> {
    private MyClass() {}
}
