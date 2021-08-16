/*
 * @test /nodynamiccopyright/
 * @bug 4914724 8048803
 * @summary Ensure that a supplementary character that cannot be the part of a Java
 *          identifier causes a compilation failure, if it is used as the part of an
 *          identifier
 * @author Naoto Sato
 *
 * @compile/fail/ref=SupplementaryJavaID5.out -XDrawDiagnostics  SupplementaryJavaID5.java
 */

public class SupplementaryJavaID5 {
    // U+1D100 (\ud834\udd00): MUSICAL SYMBOL SINGLE BARLINE (can be none of start nor part)
    int abc\ud834\udd00;
}
