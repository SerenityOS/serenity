/*
 * @test /nodynamiccopyright/
 * @summary smoke test for inference of throws type variables
 * @compile/fail/ref=TargetType63.out -XDrawDiagnostics TargetType63.java
 */
class TargetType63 {

    interface F<T extends Throwable> {
        void m() throws T;
    }

    void g1() { }
    void g2() throws ClassNotFoundException { }
    void g3() throws Exception { }

    <Z extends Throwable> void m1(F<Z> fz) throws Z { }
    <Z extends ClassNotFoundException> void m2(F<Z> fz) throws Z { }

    void test1() {
        m1(()->{ }); //ok (Z = RuntimeException)
        m1(this::g1); //ok (Z = RuntimeException)
    }

    void test2() {
        m2(()->{ }); //fail (Z = ClassNotFoundException)
        m2(this::g1); //fail (Z = ClassNotFoundException)
    }

    void test3() {
        m1(()->{ throw new ClassNotFoundException(); }); //fail (Z = ClassNotFoundException)
        m1(this::g2); //fail (Z = ClassNotFoundException)
        m2(()->{ throw new ClassNotFoundException(); }); //fail (Z = ClassNotFoundException)
        m2(this::g2); //fail (Z = ClassNotFoundException)
    }

    void test4() {
        m1(()->{ throw new Exception(); }); //fail (Z = Exception)
        m1(this::g3); //fail (Z = Exception)
    }
}
