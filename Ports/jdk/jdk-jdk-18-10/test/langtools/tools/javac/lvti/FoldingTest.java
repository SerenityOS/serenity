/*
 * @test /nodynamiccopyright/
 * @bug 8177466
 * @summary Add compiler support for local variable type-inference
 * @compile/fail/ref=FoldingTest.out -XDrawDiagnostics FoldingTest.java
 */
class FoldingTest {

        void testReachability() {
        for(var i = 0; i < 3; i++) {
              // ok
        }
            System.out.println("foo");   //this should be reachable
        }

    void testCase(String s) {
        var c = "";
        final String c2 = "" + c;
        switch (s) {
            case c: break; //error!
            case c2: break; //error!
        }
    }

    void testAnno() {
        @Anno1(s1) //error
        var s1 = "";
        @Anno2(s2) //error
        var s2 = "";
        @Anno3(s3) //error
        var s3 = "";
    }

    @interface Anno1 {
        String value();
    }
    @interface Anno2 {
        Class<?> value();
    }
    @interface Anno3 {
        Foo value();
    }

    enum Foo {
        A, B;
    }
}
