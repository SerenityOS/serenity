/*
 * @test /nodynamiccopyright/
 * @bug 8255968
 * @summary Confusing error message for inaccessible constructor
 * @run compile/fail/ref=T8255968_2.out -XDrawDiagnostics T8255968_2.java
 */

class T8255968_2 {
    T8255968_2_Test c = new T8255968_2_Test(0);
}

class T8255968_2_Test {
    T8255968_2_Test(String x) {}  // If this method is private, compiler will output the same error message.
}
