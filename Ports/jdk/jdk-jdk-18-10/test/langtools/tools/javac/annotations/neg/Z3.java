/*
 * @test /nodynamiccopyright/
 * @bug 4865660 8054556
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z3.out -XDrawDiagnostics  Z3.java
 */

enum Color { red, green, blue }

class Colored {
    Color value() default Color.red;
}
