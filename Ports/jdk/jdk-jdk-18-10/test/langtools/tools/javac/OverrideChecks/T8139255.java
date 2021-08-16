/*
 * @test    /nodynamiccopyright/
 * @bug     8139255
 * @summary javac emits diagnostic message as overriding instead of hiding for class methods.
 * @compile/fail/ref=T8139255.out -XDrawDiagnostics  T8139255.java
 */

public class T8139255 {
   static void func() { }
}

class T8139255_1  extends T8139255 {
   static int func() { return 0; }
}
