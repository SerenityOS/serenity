/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_16.out -XDrawDiagnostics T8255968_16.java
 */

class T8255968_16 {
    T8255968_16_TestMethodReference c = T8255968_16_Test::new;
}

interface T8255968_16_TestMethodReference {
    T8255968_16_Test create(int x);
}

class T8255968_16_Test {
    T8255968_16_Test(String x) {}  // If this method is private, compiler will output the same error message.
    private T8255968_16_Test(int[] x) {}
    private T8255968_16_Test(int x) {}  // This method is at the end.
}
