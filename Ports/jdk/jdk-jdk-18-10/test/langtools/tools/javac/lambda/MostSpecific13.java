/*
 * @test /nodynamiccopyright/
 * @bug 8034223
 * @summary Most-specific testing with inference variables in function parameter types
 * @compile/fail/ref=MostSpecific13.out -XDrawDiagnostics MostSpecific13.java
 */
class MostSpecific13 {

    interface UnaryOp<T> { T apply(T arg); }
    interface IntegerToNumber { Number apply(Integer arg); }

    <T> void m(UnaryOp<T> f) {}
    void m(IntegerToNumber f) {}

    void test() {
        m((Integer i) -> i); // error
        m(this::id); // error
    }

    Integer id(Integer arg) { return arg; }
}