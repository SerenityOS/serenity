/*
 * @test /nodynamiccopyright/
 * @bug 8153884
 * @summary Expression lambda erroneously compatible with void-returning descriptor
 * @compile/fail/ref=T8153884.out -XDrawDiagnostics T8153884.java
 */

class T8153884 {
    void test() {
        Runnable r = () -> (foo());
    }

    void foo() { }
}
