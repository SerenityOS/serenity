/*
 * @test /nodynamiccopyright/
 * @bug 5009601
 * @summary enum's cannot be explicitly declared final
 * @author Joseph D. Darcy
 * @compile/fail/ref=ExplicitlyFinalEnum2.out -XDrawDiagnostics ExplicitlyFinalEnum2.java
 */

final enum ExplicitlyFinalEnum2 {
    FE {
        void foo() {return;}
    };

    abstract void foo();
}
