/*
 * @test /nodynamiccopyright/
 * @bug 7030687
 * @summary Diamond: compiler accepts erroneous code where diamond is used with non-generic inner class
 * @compile/fail/ref=T7030687.out -XDrawDiagnostics T7030687.java
 */

class T7030687<X> {
    class Member { }
    static class Nested {}

    void test() {
        class Local {}

        Member m = new Member<>();
        Nested n = new Nested<>();
        Local l = new Local<>();
    }
}
