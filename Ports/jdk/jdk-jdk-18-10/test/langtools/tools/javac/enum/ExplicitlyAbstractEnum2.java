/*
 * @test /nodynamiccopyright/
 * @bug 5009601
 * @summary enum's cannot be explicitly declared abstract even if they are abstract
 * @author Joseph D. Darcy
 * @compile/fail/ref=ExplicitlyAbstractEnum2.out -XDrawDiagnostics ExplicitlyAbstractEnum2.java
 */

abstract enum ExplicitlyAbstractEnum2 {
    FE {
        void foo() {return;}
    };

    abstract void foo();
}
