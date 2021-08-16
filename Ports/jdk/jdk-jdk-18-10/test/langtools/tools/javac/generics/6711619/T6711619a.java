/*
 * @test /nodynamiccopyright/
 * @bug 6711619
 *
 * @summary javac doesn't allow access to protected members in intersection types
 * @author Maurizio Cimadamore
 *
 * @compile/fail/ref=T6711619a.out -XDrawDiagnostics T6711619a.java
 */
class T6711619a {

    static class A {
        private void a() {}
        private A a;
    }
    static class B extends A {
        private B b() {}
        private B b;
    }
    static interface I{
        void i();
    }
    static interface I1{
        void i1();
    }
    static class E extends B implements I, I1{
        public void i() {}
        public void i1() {}
    }
    static class C<W extends B & I1, T extends W>{
        T t;
        W w;
        C(W w, T t) {
            this.w = w;
            this.t = t;
        }
    }

    static void testMemberMethods(C<? extends A, ? extends I> arg) {
        arg.t.a();
        arg.t.b();
    }

    static void testMemberFields(C<? extends A, ? extends I> arg) {
        A ta; B tb;
        ta = arg.t.a;
        tb = arg.t.b;
        ta = arg.w.a;
        tb = arg.w.b;
    }
}
