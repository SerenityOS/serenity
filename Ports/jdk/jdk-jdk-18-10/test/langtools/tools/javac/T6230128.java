/*
 * @test  /nodynamiccopyright/
 * @bug 6230128
 * @compile/fail/ref=T6230128.out -XDrawDiagnostics T6230128.java
 */
class A1 {
    public void foo(Object[] args) { }
}

class A1a extends A1 {
    void foo(Object... args) { }
}
