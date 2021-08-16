/*
 * @test    /nodynamiccopyright/
 * @bug     6393539
 * @summary no compile-time error for clone, etc. in annotation type
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=NoObjectMethods.out -XDrawDiagnostics  NoObjectMethods.java
 */

public @interface NoObjectMethods {
    int clone();
}
