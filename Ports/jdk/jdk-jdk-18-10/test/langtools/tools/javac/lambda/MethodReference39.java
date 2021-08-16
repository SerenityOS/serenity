/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that bad enclosing class parameter type is discarded accordingly
 * @compile/fail/ref=MethodReference39.out -XDrawDiagnostics MethodReference39.java
 */
class MethodReference39 {

    static class Sup {}


    static class Sub extends Sup {

        interface SAM { Sup m(Sup x, String str); }

        class Inner extends Sup {
            Inner(String val) { }
        }

        void test() {
            SAM var = Sub.Inner::new;;
        }
    }
}
