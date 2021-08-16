/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_12.out -XDrawDiagnostics T8255968_12.java
 */

class T8255968_12 {
    T8255968_12_TestMethodReference c = T8255968_12_Test::new;
}

interface T8255968_12_TestMethodReference {
    T8255968_12_Test create(int x);
}

class T8255968_12_Test {
    private T8255968_12_Test(int x) {}  // This method is not at the end.
    T8255968_12_Test(String x) {}  // If this method is private, compiler will output the same error message.
}
