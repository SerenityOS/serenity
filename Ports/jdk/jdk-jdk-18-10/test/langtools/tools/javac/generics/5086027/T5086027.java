/*
 * @test    /nodynamiccopyright/
 * @bug     5086027
 * @summary Inner class of generic class cannot extend Throwable
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T5086027.out -XDrawDiagnostics  T5086027.java
 */

public class T5086027<T> {
    class X extends Exception {}
}
