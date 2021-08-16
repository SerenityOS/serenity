/**
 * @test  /nodynamiccopyright/
 * @bug 6476118 7170058
 * @summary compiler bug causes runtime ClassCastException for generics overloading
 * @compile/fail/ref=T6476118a.out -XDrawDiagnostics T6476118a.java
 */

class T6476118a {
    static class A {
        public int compareTo(Object o) { return 0; }
    }

    static class B extends A implements Comparable<B>{
        public int compareTo(B b){ return 0; }
    }
}
