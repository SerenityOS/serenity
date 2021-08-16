/*
 * @test /nodynamiccopyright/
 * @bug 8005166
 * @summary Add support for static interface methods
 *          smoke test for static interface methods
 * @compile/fail/ref=Static02.out -XDrawDiagnostics -XDallowStaticInterfaceMethods Static02.java
 */
class Static02 {

    interface I {
        public static void test() { }
    }

    public static void main(String[] args) {
        I.test(); //ok
        I i = new I() {};
        i.test(); //no!
    }
}
