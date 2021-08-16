/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that unbound constructor reference are not accepted
 * @compile/fail/ref=MethodReference40.out -XDrawDiagnostics MethodReference40.java
 */
class MethodReference40 {

    static class Sup {
        class Inner {
            Inner(String val) { }
        }
    }

    static class Sub extends Sup {

        interface SAM { Sup.Inner m(Sub x, String str); }

        void test() {
            SAM var = Sub.Inner::new;
        }
    }
}
