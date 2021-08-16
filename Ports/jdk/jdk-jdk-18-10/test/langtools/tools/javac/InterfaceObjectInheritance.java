/*
 * @test /nodynamiccopyright/
 * @bug 4526026
 * @summary javac allows access to interface members inherited protected from Object
 * @author gafter
 *
 * @compile/fail/ref=InterfaceObjectInheritance.out -XDrawDiagnostics  InterfaceObjectInheritance.java
 */

interface InterfaceObjectInheritance {
    class Inner {
        static void bar(InterfaceObjectInheritance i) {
            try {
                // An inner class has access to any protected members, but
                // according to JLS 9.2, an interface has no protected members,
                // so this reference to finalize should not compile.
                i.finalize();
            } catch (Throwable t) {
            }
        }
    }
}
