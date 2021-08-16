/*
 * @test /nodynamiccopyright/
 * @bug 8002286
 * @summary Resolve should support nested resolution contexts
 * @compile/fail/ref=T8002286.out -XDrawDiagnostics T8002286.java
 */
class T8002286 {
    @Anno(nonExistent())
    static class Test { }

    @interface Anno { }
}
