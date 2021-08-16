/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_15.out -XDrawDiagnostics T8255968_15.java
 */

class T8255968_15 {
    T8255968_15_TestMethodReference c = T8255968_15_Test::new;
}

interface T8255968_15_TestMethodReference {
    T8255968_15_Test create(int x);
}

class T8255968_15_Test {
    T8255968_15_Test(String x) {}  // If this method is private, compiler will output the same error message.
    private T8255968_15_Test(int x) {}  // This method is not at the end.
    private T8255968_15_Test(int[] x) {}
}
