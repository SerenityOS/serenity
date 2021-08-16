/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_10.out -XDrawDiagnostics T8255968_10.java
 */

class T8255968_10 {
    T8255968_10_TestMethodReference c = T8255968_10_Test::new;
}

interface T8255968_10_TestMethodReference {
    T8255968_10_Test create(int x);
}

class T8255968_10_Test {
    private T8255968_10_Test(int x) {}
}
