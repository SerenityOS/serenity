/*
 * @test    /nodynamiccopyright/
 * @bug     6227936
 * @summary Wrong type of inherited method using specialized type parameter
 * @compile/fail/ref=Orig.out -XDrawDiagnostics  Orig.java
 */

class GenericTest {
    static class A<T extends B> {
        T myB;
        A(T myB) {this.myB = myB;}
        T getB() {return myB;}
    }
    static class B<T extends C> {
        T myC;
        B(T myB) {this.myC = myC;}
        T getC() {return myC;}
    }
    static class C {
        C() {}
    }

    static class A1<T extends B1> extends A<T> {
        A1(T myB) {super(myB);}
        public void testMethod() {
            // This next line fails, but should work
            getB().getC().someMethod();
            ((C1)getB().getC()).someMethod();
        }
    }
    static class B1<T extends C1> extends B<T> {
        B1(T myC) {super(myC);}
    }
    static class C1 extends C {
        public void someMethod() {}
    }
}
