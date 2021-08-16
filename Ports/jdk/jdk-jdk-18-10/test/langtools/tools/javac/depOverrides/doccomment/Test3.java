/*
 * @test  /nodynamiccopyright/
 * @bug 5086088
 * @summary check warnings generated when overriding deprecated methods
 *
 * @compile/ref=Test3.out -XDrawDiagnostics -Xlint:deprecation Test3.java
 */

interface LibInterface {
    /** @deprecated */
        void m();
}

class LibClass {
    public void m() { }
}

class Test3 extends LibClass implements LibInterface {
}
