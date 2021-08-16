/*
 * @test    /nodynamiccopyright/
 * @bug     6393539
 * @summary no compile-time error for clone, etc. in annotation type
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=NoClone.out -XDrawDiagnostics  NoClone.java
 */

public @interface NoClone {
    Object clone();
}
