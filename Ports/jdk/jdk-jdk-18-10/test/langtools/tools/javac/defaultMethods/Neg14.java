/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that a class cannot have two sibling interfaces with a default and abstract method
 * @compile/fail/ref=Neg14.out -XDrawDiagnostics Neg14.java
 */
class Neg14 {
    interface IA { int m(); }
    interface IB { default int m() { return 1; } }

    abstract class AB implements IA, IB {}
}
