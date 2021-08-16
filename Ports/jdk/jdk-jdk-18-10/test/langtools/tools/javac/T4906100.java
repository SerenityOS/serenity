/*
 * @test  /nodynamiccopyright/
 * @bug 4906100
 * @summary detect empty statement after if
 * @compile/ref=T4906100.out -XDrawDiagnostics -Xlint:empty T4906100.java
 */

class T4906100 {
    void f1(int a, int b) {
        if (a == b);
            System.out.println("a == b");
    }

    @SuppressWarnings("empty")
    void f2(int a, int b) {
        if (a == b);
            System.out.println("a == b");
    }

    // check that { } is not treated as an empty statement
    void f3(int a, int b) {
        if (a == b) { }
            System.out.println("a == b");
    }
}
