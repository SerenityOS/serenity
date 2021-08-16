/*
 * @test /nodynamiccopyright/
 * @bug 4912075
 * @summary static import of private field crashes compiler
 * @author gafter
 *
 * @compile/fail/ref=PrivateStaticImport.out -XDrawDiagnostics   PrivateStaticImport.java
 */

package psi;

import static psi.Foo.*;

class Foo {
    private static int FOO_VALUE = 55;
}
class Bar {
    int value = FOO_VALUE;
}
