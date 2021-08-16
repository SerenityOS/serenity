/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that non-boxing method references is not preferred over boxing one
 * @compile/fail/ref=MethodReference25.out -XDrawDiagnostics MethodReference25.java
 */

class MethodReference25 {

    static void m(Integer i) { }

    interface SAM1 {
        void m(int x);
    }

    interface SAM2 {
        void m(Integer x);
    }

    static void call(int i, SAM1 s) { s.m(i);  }
    static void call(int i, SAM2 s) { s.m(i);  }

    public static void main(String[] args) {
        call(1, MethodReference25::m); //ambiguous
    }
}
