/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z1.out -XDrawDiagnostics  Z1.java
 */

enum Color { red, green, blue }

@interface Colored {
    Color value();
}

@Colored(teal)
class Martian {
}
