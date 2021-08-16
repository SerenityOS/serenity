/*
 * @test /nodynamiccopyright/
 * @bug 8066974 8062373
 * @summary Compiler doesn't infer method's generic type information in lambda body
 * @compile/fail/ref=T8066974.out -XDrawDiagnostics T8066974.java
 */
class T8066974 {
    static class Throwing<E extends Throwable> { }
    static class RuntimeThrowing extends Throwing<RuntimeException> { }
    static class CheckedThrowing extends Throwing<Exception> { }

    interface Parameter {
        <E extends Throwable> Object m(Throwing<E> tw) throws E;
    }

    interface Mapper<R> {
        R m(Parameter p);
    }

    <Z> Z map(Mapper<Z> mz) { return null; }

    <Z extends Throwable> Mapper<Throwing<Z>> mapper(Throwing<Z> tz) throws Z { return null; }

    static class ThrowingMapper<X extends Throwable> implements Mapper<Throwing<X>> {
        ThrowingMapper(Throwing<X> arg) throws X { }

        @Override
        public Throwing<X> m(Parameter p) {
        return null;
        }
    }

    void testRuntime(RuntimeThrowing rt) {
        map(p->p.m(rt));
        map(mapper(rt));
        map(new ThrowingMapper<>(rt));
        map(new ThrowingMapper<>(rt) {});
    }

    void testChecked(CheckedThrowing ct) {
        map(p->p.m(ct));
        map(mapper(ct));
        map(new ThrowingMapper<>(ct));
        map(new ThrowingMapper<>(ct) {});
    }
}
