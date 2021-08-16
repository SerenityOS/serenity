/*
 * @test /nodynamiccopyright/
 * @bug 4906400
 * @summary (JSR175) compiler allows self-containing annotation types
 * @author gafter
 *
 * @compile/fail/ref=Cycle1.out -XDrawDiagnostics  Cycle1.java
 */

package cycle1;

@interface Foo {
    Foo foo();
}
