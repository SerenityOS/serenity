/*
 * @test /nodynamiccopyright/
 * @bug 4914724
 * @summary Ensure that the invalid surrogate sequence, as the part of an identifier,
 *          causes a compilation failure
 * @author Naoto Sato
 *
 * @compile/fail/ref=SupplementaryJavaID3.out -XDrawDiagnostics  SupplementaryJavaID3.java
 */

public class SupplementaryJavaID3 {
    int abc\ud801\ud801;
}
