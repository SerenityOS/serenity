/*
 * @test /nodynamiccopyright/
 * @bug 8132535
 * @summary Compiler fails with diamond anonymous class creation with intersection bound of enclosing class.
 * @compile/fail/ref=Neg21.out Neg21.java -XDrawDiagnostics
 */

public class Neg21 <T extends java.io.Serializable & Cloneable> {

    class A <X>{}

    public void foo(){
        new Neg21<>().new A<>(){} ;
    }
}
