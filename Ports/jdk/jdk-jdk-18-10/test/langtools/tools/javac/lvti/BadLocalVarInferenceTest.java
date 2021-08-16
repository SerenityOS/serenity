/*
 * @test /nodynamiccopyright/
 * @bug 8177466 8191834
 * @summary Add compiler support for local variable type-inference
 * @compile/fail/ref=BadLocalVarInferenceTest.out -XDrawDiagnostics BadLocalVarInferenceTest.java
 */

class BadLocalVarInferenceTest {

    interface Foo<X> {
        void m(X x);
    }

    interface Supplier<X> {
        void m(X x);
    }

    void test() {
        var x;
        var f = () -> { };
        var m = this::l;
        var g = null;
        var d = d = 1;
        var k = { 1 , 2 };
        var l = new Foo<>() { //LHS was Foo<String>
            @Override
            void m(String s) { }
        };
        var s = f(x -> { x.charAt(0); }); //LHS was String
        var t = m(); //void
    }

    <Z> Z f(Supplier<Z> sz) { return null; }

    void m() { }
}
