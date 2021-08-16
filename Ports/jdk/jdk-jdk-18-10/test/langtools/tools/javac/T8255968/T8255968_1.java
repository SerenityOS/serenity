/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_1.out -XDrawDiagnostics T8255968_1.java
 */

class T8255968_1 {
    T8255968_1_Test c = new T8255968_1_Test(0);
}

class T8255968_1_Test {
    private T8255968_1_Test(int x) {}
}
