/*
 * @test  /nodynamiccopyright/
 * @bug 8194998
 * @summary broken error message for subclass of interface with private method
 * @compile/fail/ref=BrokenErrorMessageTest.out -XDrawDiagnostics BrokenErrorMessageTest.java
 */

class BrokenErrorMessageTest {
    void foo() {
        // there is no error in this case but it is an interesting test, ::test is a member of I so this is acceptable
        Runnable test1 = ((I)(new I() {}))::test;
        // ::test is not a member of any subclass of I as it is private
        Runnable test2 = ((new I() {}))::test;
    }

    interface I {
        private void test() {}
    }
}
