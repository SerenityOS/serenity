/*
 * @test    /nodynamiccopyright/
 * @bug     4022674
 * @summary Compiler should detect throws-clauses' conflict.
 * @author  turnidge
 *
 * @compile/fail/ref=ThrowsConflict.out -XDrawDiagnostics  ThrowsConflict.java
 */

interface I {
    void method();
}

class A {
    public void method() throws Exception {
    }
}

public
class ThrowsConflict extends A implements I {
}
