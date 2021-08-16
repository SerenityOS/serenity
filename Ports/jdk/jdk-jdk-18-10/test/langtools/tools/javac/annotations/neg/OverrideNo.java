/*
 * @test /nodynamiccopyright/
 * @bug 4901275 4989669
 * @summary JSR175 (7): implement <at>Overrides
 * @author gafter
 *
 * @compile/fail/ref=OverrideNo.out -XDrawDiagnostics  OverrideNo.java
 */

package overrideNo;

abstract class A {
}

class B extends A {
    @Override void f() {}
}
