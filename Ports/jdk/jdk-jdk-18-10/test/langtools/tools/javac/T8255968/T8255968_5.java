/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_5.out -XDrawDiagnostics T8255968_5.java
 */

class T8255968_5 {
    T8255968_5_Test c = new T8255968_5_Test(0);
}

class T8255968_5_Test {
    private T8255968_5_Test(int x) {}  // This method is not at the end.
    T8255968_5_Test(String x) {}  // If this method is private, compiler will output the same error message.
    private T8255968_5_Test(int[] x) {}
}
