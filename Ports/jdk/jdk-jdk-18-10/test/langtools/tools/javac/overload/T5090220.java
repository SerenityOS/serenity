/*
 * @test /nodynamiccopyright/
 * @bug 5090220
 * @summary Autoboxing applied when calculating most-specific method
 * @compile/fail/ref=T5090220.out -XDrawDiagnostics  T5090220.java
 */

class T5090220 {
    static void foo(int i1, Integer i2) {
        System.out.println("Integer");
    }
    static void foo(Integer i1, double d) {
        System.out.println("double");
    }
    public static void main(String[] args) {
        foo(5, 5);
    }
}
