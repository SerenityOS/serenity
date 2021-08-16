/*
 * @test /nodynamiccopyright/
 * @bug 7132880
 * @summary Resolve should support nested resolution contexts
 * @compile/fail/ref=T7132880.out -XDrawDiagnostics T7132880.java
 */
class Outer {
    void m1(String s) { }
    void m2(int i1, int i2) { }

    class Inner {
        void test() {
           //ok - no method named 'm' in Inner - hence, class to search is Outer
           m1("");
        }
    }

    class Inner1 {
        void m1(Integer i) { }

        void test() {
           //error - Inner1 defines an incompatible method - hence, class to search is Inner1
           m1("");
        }
    }

    class Inner2 {
        private void m1(Integer i) { }
        private void m1(Double d) { }

        void test() {
           //error - Inner2 defines multiple incompatible methods - hence, class to search is Inner2
           m1("");
        }
    }

    class Inner3 {
        private void m2(Object o, int i) { }
        private void m2(int i, Object o) { }

        void test() {
           //error - Inner3 defines multiple ambiguous methods - hence, class to search is Inner3
           m2(1, 1);
        }
    }

    class Inner4 extends Inner2 {
        void test() {
           //ok - Inner2 defines multiple incompatible inaccessible methods - hence, class to search is Outer
           m1("");
        }
    }

    class Inner5 extends Inner3 {
        void test() {
           //ok - Inner3 defines multiple inaccessible ambiguous methods - hence, class to search is Outer
           m2(1, 1);
        }
    }
}
