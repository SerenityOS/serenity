/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_4.out -XDrawDiagnostics T8255968_4.java
 */

class T8255968_4 {
    T8255968_4_Test c = new T8255968_4_Test(0);
}

class T8255968_4_Test {
    T8255968_4_Test(String x) {}  // If this method is private, compiler will output the same error message.
    private T8255968_4_Test(int x) {}  // This method is at the end.
}
