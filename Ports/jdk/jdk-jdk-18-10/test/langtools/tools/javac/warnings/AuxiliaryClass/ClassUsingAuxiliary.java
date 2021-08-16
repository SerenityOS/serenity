/**
 * @test /nodynamiccopyright/
 * @bug 7153951
 * @clean ClassUsingAuxiliary ClassWithAuxiliary AuxiliaryClass ClassWithAuxiliary$NotAnAuxiliaryClass ClassWithAuxiliary$NotAnAuxiliaryClassEither
 * @run compile ClassUsingAuxiliary.java ClassWithAuxiliary.java
 * @run compile/fail/ref=ClassUsingAuxiliary1.out -XDrawDiagnostics -Werror -Xlint:auxiliaryclass ClassUsingAuxiliary.java ClassWithAuxiliary.java
 * @run compile/fail/ref=ClassUsingAuxiliary2.out -XDrawDiagnostics -Werror -Xlint:auxiliaryclass ClassUsingAuxiliary.java
 */

class ClassUsingAuxiliary {
    AuxiliaryClass ahem;
}
