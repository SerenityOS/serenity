/*
 * @test /nodynamiccopyright/
 * @bug 7090499
 * @summary missing rawtypes warnings in anonymous inner class
 * @compile/ref=T7090499.out -Xlint:rawtypes -XDrawDiagnostics T7090499.java
 */

class T7090499<X> {
    {
        new Object() {
            T7090499 x;
        };
    }
}
