/**
 * @test  /nodynamiccopyright/
 * @bug 6476118 6533652 7170058
 * @summary compiler bug causes runtime ClassCastException for generics overloading
 * @compile/fail/ref=T6476118b.out -XDrawDiagnostics T6476118b.java
 */

class T6476118b {
    public final int compareTo(Object o) { return 0; }

    static class B extends T6476118b implements Comparable<B> {
        public int compareTo(B b){ return 0; }
    }
}
