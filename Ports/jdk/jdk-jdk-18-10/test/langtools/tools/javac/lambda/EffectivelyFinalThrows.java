/*
 * @test /nodynamiccopyright/
 * @bug 8019521
 * @summary Check that enhanced rethrow/effectivelly final works correctly inside lambdas
 * @compile EffectivelyFinalThrows.java
 */

class EffectivelyFinalThrows {
    interface SAM<E extends Throwable> {
        public void t() throws E;
    }
    <E extends Throwable> void test(SAM<E> s) throws E {
        s.t();
    }
    void test2(SAM<Checked> s) throws Checked {
        test(() -> {
            try {
                s.t();
            } catch (Throwable t) {
                throw t;
            }
        });
    }
    static class Checked extends Exception {}
}
