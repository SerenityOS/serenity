/*
 * @test /nodynamiccopyright/
 * @bug 6663588
 * @summary Compiler goes into infinite loop for Cyclic Inheritance test case
 * @author Maurizio Cimadamore
 * @compile/fail/ref=T6663588.out -XDrawDiagnostics T6663588.java
 */

public class T6663588<T extends T6663588.Inner> extends T6663588 {
     class Inner extends T6663588.Inner {}
}
