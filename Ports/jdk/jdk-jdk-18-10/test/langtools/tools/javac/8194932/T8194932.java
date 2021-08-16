/*
 * @test /nodynamiccopyright/
 * @bug 8194932
 * @summary no ambuguity error is emitted if classfile contains two identical methods with different return types
 * @build Foo
 * @compile/fail/ref=T8194932.out -XDrawDiagnostics T8194932.java
 */

class T8194932 {
    void test(Foo foo) {
        foo.m(); //should get an ambiguity here
    }
}
