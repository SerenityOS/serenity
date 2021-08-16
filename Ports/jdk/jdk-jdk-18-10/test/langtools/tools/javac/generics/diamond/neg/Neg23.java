/*
 * @test /nodynamiccopyright/
 * @bug 8132535
 * @summary Compiler fails with diamond anonymous class creation with intersection bound of enclosing class.
 * @compile/fail/ref=Neg23.out Neg23.java -XDrawDiagnostics
 */

public class Neg23  {
    {
        new pkg.Neg23_01<>().new Inner<>();
    }
}
