/*
 * @test  /nodynamiccopyright/
 * @bug 5048776
 * @compile/ref=T5048776a.out -XDrawDiagnostics                  T5048776.java
 * @compile/ref=T5048776b.out -XDrawDiagnostics -Xlint:all,-path T5048776.java
 */
class A1 {
    void foo(Object[] args) { }
}

class A1a extends A1 {
    void foo(Object... args) { }
}

class A2 {
    void foo(Object... args) { }
}

class A2a extends A2 {
    void foo(Object[] args) { }
}
