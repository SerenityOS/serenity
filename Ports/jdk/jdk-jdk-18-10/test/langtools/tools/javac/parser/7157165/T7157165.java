/*
 * @test /nodynamiccopyright/
 * @bug 7157165
 *
 * @summary Regression: code with disjunctive type crashes javac
 * @compile/fail/ref=T7157165.out -XDrawDiagnostics T7157165.java
 *
 */

class T7157165 {
    Foo<? extends A|B> foo1 = null;
}
