/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  compiler throw AssertionError while backtracing from speculative attribution round
 * @compile/fail/ref=TargetType44.out -XDrawDiagnostics TargetType44.java
 */
class TargetType44 {

    interface Unary {
        void m(int i1);
    }

    interface Binary {
        void m(int i1, int i2);
    }

    void m(Unary u) { }
    void m(Binary u) { }

    void test() {
        m(()-> { new Object() { }; }); //fail
        m(x -> { new Object() { }; }); //ok
        m((x, y) -> { new Object() { }; }); //ok
        m((x, y, z) -> { new Object() { }; }); //fail
    }
}
