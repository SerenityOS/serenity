/*
 * @test /nodynamiccopyright/
 * @bug 4906400
 * @summary (JSR175) compiler allows self-containing annotation types
 * @author gafter
 *
 * @compile/fail/ref=Cycle2.out -XDrawDiagnostics  Cycle2.java
 */

package cycle2;

@interface Bar {
    Baz baz();
}

@interface Baz {
    Bar bar();
}
