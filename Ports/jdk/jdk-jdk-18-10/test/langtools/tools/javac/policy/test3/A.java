/* /nodynamiccopyright/ */

class A {
    void m1() {
        System.err.println("hello");
        0 // syntax error
        System.err.println("world");
    }

    void m2() {
    }
}
