/*
 * @test /nodynamiccopyright/
 * @bug 6993963
 *
 * @summary Project Coin: Use precise exception analysis for effectively final catch parameters
 * @author mcimadamore
 * @compile Pos06.java
 *
 */

class Pos06 {
    static class A extends Exception {}
    static class B extends Exception {}

    void m() {
        try {
            if (true) {
                throw new A();
            }
            else {
                throw new B();
            }
        } catch (A | B ex) {
            System.out.println(ex);
        }
    }
}
