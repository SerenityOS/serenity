/*
 * @test /nodynamiccopyright/
 * @bug 4914724 8048803
 * @summary Ensure that a supplementary character that cannot be the start of a Java
 *          identifier causes a compilation failure, if it is used as the start of an
 *          identifier
 * @author Naoto Sato
 *
 * @compile/fail/ref=SupplementaryJavaID4.out -XDrawDiagnostics  SupplementaryJavaID4.java
 */

public class SupplementaryJavaID4 {
    // U+1D17B (\ud834\udd7b): MUSICAL SYMBOL COMBINING ACCENT (can only be part)
    int \ud834\udd7b;
}
