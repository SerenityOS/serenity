/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z16.out -XDrawDiagnostics  Z16.java
 */

enum Color { red, green, blue }

@interface Colored {
    Color value() default redx;
}
