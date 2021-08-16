/*
 * @test /nodynamiccopyright/
 * @bug 4914724
 * @summary Ensure that the invalid surrogate sequence, as the start of an identifier,
 *          causes a compilation failure
 * @author Naoto Sato
 *
 * @compile/fail/ref=SupplementaryJavaID2.out -XDrawDiagnostics  SupplementaryJavaID2.java
 */

public class SupplementaryJavaID2 {
    int \ud801\ud801abc;
}
