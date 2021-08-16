/*
 * @test /nodynamiccopyright/
 * @bug 4094658
 * @summary Test enforcement of JLS 6.6.1 and 6.6.2 rules requiring that
 * the type to which a component member belongs be accessible in qualified
 * names.
 * @compile/fail/ref=QualifiedAccess_4.out -XDrawDiagnostics QualifiedAccess_4.java
 */

import pack1.P1;

class CMain {

    class Foo {
        class Bar {}
    }
    Foo.Bar yy  = x.new Foo.Bar();      // ERROR - Type in qualified 'new' must be unqualified
}
