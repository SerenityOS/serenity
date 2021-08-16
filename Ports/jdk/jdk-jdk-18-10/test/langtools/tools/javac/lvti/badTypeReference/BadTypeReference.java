/*
 * @test /nodynamiccopyright/
 * @bug 8177466
 * @summary Add compiler support for local variable type-inference
 * @compile -source 8 pkg/var.java
 * @compile pkg/nested/var/A.java
 * @compile/fail/ref=BadTypeReference.out -XDrawDiagnostics BadTypeReference.java
 */

import pkg.*;

public class BadTypeReference {
    void test(Object o) {
        var<String> vs = null; //error
        Object o2 = var.x; //error
        pkg.nested.var.A a = new pkg.nested.var.A(); //ok
    }
}
