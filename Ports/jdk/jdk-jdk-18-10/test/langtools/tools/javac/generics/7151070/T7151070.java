/*
 * @test /nodynamiccopyright/
 * @bug     7151070
 * @summary NullPointerException in Resolve.isAccessible
 * @compile/fail/ref=T7151070.out -XDrawDiagnostics T7151070.java
 */

class T7151070a {
    private static class PrivateCls { }
    public static class PublicCls extends PrivateCls { }

    public void m(PrivateCls p) { }
}

class T7151070b {
    public void test(Test<T7151070a.PublicCls> obj, T7151070a outer) {
        outer.m(obj.get());
    }

    public static class Test<T> {
        public T get() {
            return null;
        }
    }
}
