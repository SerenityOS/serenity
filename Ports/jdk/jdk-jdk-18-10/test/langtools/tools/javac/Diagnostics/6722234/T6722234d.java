/**
 * @test /nodynamiccopyright/
 * @bug     6722234 8078024
 * @summary javac diagnostics need better integration with the type-system
 * @author  mcimadamore
 * @compile/fail/ref=T6722234d_1.out -XDrawDiagnostics --diags=formatterOptions=where T6722234d.java
 * @compile/fail/ref=T6722234d_2.out -XDrawDiagnostics --diags=formatterOptions=where,simpleNames T6722234d.java
 */

class T6722234d {
    interface I1 {}
    interface I2 {}
    class A implements I1, I2 {}
    class B implements I1, I2 {}
    class Test {
        <Z> Z m(Z z1, Z z2) { return null; }
        void main(){
            A a = m(new A(), new B());
        }
    }
}
