/*
 * @test /nodynamiccopyright/
 * @bug 8008813
 * @summary Structural most specific fails when method reference is passed to overloaded method
 * @compile/fail/ref=MostSpecific08.out -XDrawDiagnostics MostSpecific08.java
 */
class MostSpecific08 {

    static class C {
        int getInt() { return -1; }
        Integer getInteger() { return -1; }
    }

    interface IntResult { }
    interface ReferenceResult<X> { }

    interface PrimitiveFunction {
        int f(C c);
    }

    interface ReferenceFunction<X> {
        X f(C c);
    }

    interface Tester {
        IntResult apply(PrimitiveFunction p);
        <Z> ReferenceResult<Z> apply(ReferenceFunction<Z> p);
    }

    void testMref(Tester t) {
        IntResult pr = t.apply(C::getInt); //ok - unoverloaded mref
        ReferenceResult<Integer> rr = t.apply(C::getInteger); //ok - unoverloaded mref
    }

    void testLambda(Tester t) {
        IntResult pr1 = t.apply(c->c.getInt()); //ambiguous - implicit
        IntResult pr2 = t.apply((C c)->c.getInt()); //ok
        ReferenceResult<Integer> rr1 = t.apply(c->c.getInteger()); //ambiguous - implicit
        ReferenceResult<Integer> rr2 = t.apply((C c)->c.getInteger()); //ok
    }
}
