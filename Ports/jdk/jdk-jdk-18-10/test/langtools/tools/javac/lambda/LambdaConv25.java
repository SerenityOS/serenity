/*
 * @test /nodynamiccopyright/
 * @summary check that merged inherited descriptors preservers type-parameters
 * @compile/fail/ref=LambdaConv25.out -XDrawDiagnostics LambdaConv25.java
 */
class LambdaConv25 {

    interface A {
        <X> void m();
    }

    interface B {
        <X> void m();
    }

    interface C extends A, B { }

    void test() {
        C c = ()->{}; //should fail
    }
}
