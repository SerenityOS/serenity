/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_6.out -XDrawDiagnostics T8255968_6.java
 */

class T8255968_6 {
    T8255968_6_Test c = new T8255968_6_Test(0);
}

class T8255968_6_Test {
    T8255968_6_Test(String x) {}  // If this method is private, compiler will output the same error message.
    private T8255968_6_Test(int x) {}  // This method is not at the end.
    private T8255968_6_Test(int[] x) {}
}
