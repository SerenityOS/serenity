/*
 * @test /nodynamiccopyright/
 * @bug 4101529
 * @summary The compiler used to create class names which were the same as
 *          existing package names and vice-versa.  The compiler now checks
 *          for this before creating a package or a class.
 * @author turnidge
 *
 * @compile/fail/ref=Bad.out -XDrawDiagnostics  Bad.java
 */

package java.lang.String;

class Bad {
}
