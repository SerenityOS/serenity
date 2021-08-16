/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_11.out -XDrawDiagnostics T8255968_11.java
 */

class T8255968_11 {
    T8255968_11_TestMethodReference c = T8255968_11_Test::new;
}

interface T8255968_11_TestMethodReference {
    T8255968_11_Test create(int x);
}

class T8255968_11_Test {
    T8255968_11_Test(String x) {}  // If this method is private, compiler will output the same error message.
}
