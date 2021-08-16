/*
 * @test /nodynamiccopyright/
 * @bug 8264258
 * @summary Unknown lookups in the java package give misleading compilation errors
 * @compile/fail/ref=MisleadingNonExistentPathError.out -XDrawDiagnostics MisleadingNonExistentPathError.java
 */
package knownpkg;

public class MisleadingNonExistentPathError {

    void classNotFound() {
        // Not found, but in an existing package
        Class<?> c1 = knownpkg.NotFound.class;

        // Not found, but in a (system) package which exists and is in scope
        Class<?> c2 = java.lang.NotFound.class;

        // Not found, on a genuinely unknown package
        Class<?> c3 = unknownpkg.NotFound.class;

        // Not found, but in the 'java' package which is in scope as per JLS 6.3 and observable as per JLS 7.4.3
        Class<?> c4 = java.NotFound.class;

        // Not found, but in a (system) package which exists and is in scope
        Object c5 = new java.lang();
    }
}
