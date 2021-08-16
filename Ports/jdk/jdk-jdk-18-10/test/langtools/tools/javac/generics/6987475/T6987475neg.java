/*
 * @test /nodynamiccopyright/
 * @bug 6987475
 *
 * @summary Order of declarations affects whether abstract method considered overridden
 * @compile/fail/ref=T6987475neg.out -XDrawDiagnostics T6987475neg.java
 */

class T6987475neg {
    static abstract class Base<A> {
        public void go(String s) { }
        public abstract void go(A a);
    }

    static abstract class BaseReverse<A> {
        public abstract void go(A a);
        public void go(String s) { }
    }

    static abstract class Sub<A> extends Base<A> {
        public abstract void go(A a);
    }
    static abstract class SubReverse<A> extends BaseReverse<A> {
        public abstract void go(A a);
    }

    static class Impl1 extends Sub<String> { }
    static class Impl2 extends SubReverse<String> { }
}
