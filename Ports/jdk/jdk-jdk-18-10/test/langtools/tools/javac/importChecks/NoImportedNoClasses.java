/*
 * @test /nodynamiccopyright/
 * @bug 7101822
 * @summary Verify that statically importing non-existing constant causes a compile-time error
 *          for files without a class.
 *
 * @compile/fail/ref=NoImportedNoClasses.out -XDrawDiagnostics NoImportedNoClasses.java
 */

import static java.lang.Runnable.UNKNOWN;
