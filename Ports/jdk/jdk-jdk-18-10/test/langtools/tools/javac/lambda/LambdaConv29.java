/*
 * @test /nodynamiccopyright/
 * @bug 8183126
 * @summary test for lambda finder
 * @compile/fail/ref=LambdaConv29.out -XDrawDiagnostics -Werror -XDfind=lambda LambdaConv29.java
 */

class LambdaConv29 {

    interface SAM {
        void m();
    }

    SAM s1 = new SAM() { public void m() {} };
}
