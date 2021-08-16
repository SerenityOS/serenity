/*
 * @test  /nodynamiccopyright/
 * @bug 4018525 4059072 4277274 4785453
 * @summary Test that recursive 'extends' and 'implements' clauses are detected
 * and disallowed.
 *
 * @compile/fail/ref=CyclicInheritance.out -XDrawDiagnostics CyclicInheritance.java
 */






class C1 extends C1 {}                  // ERROR - Cyclic inheritance

class C11 extends C12 {}                // ERROR - Cyclic inheritance
class C12 extends C11 {}                // error in previous line could correctly be reported here as well

interface I1 extends I1 {}              // ERROR - Cyclic inheritance

interface I11 extends I12 {}            // ERROR - Cyclic inheritance
interface I12 extends I11 {}            // error in previous line could correctly be reported here as well

//-----

class C211 implements C211.I {          // ERROR - may change pending resoluation of 4087020
        interface I {};                 // error in previous line could correctly be reported here as well
}

class C212 extends C212.C {             // ERROR - Cyclic inheritance, subclass cannot enclose superclass
        static class C {};              // error in previous line could correctly be reported here as well
}


class C221 implements C221.I {          // ERROR - Cannot access C21 (private)
        private interface I {};         // error in previous line could correctly be reported here as well
}

class C222 extends C222.C {             // ERROR - Cannot access C22 (private)
        private static class C {};      // error in previous line could correctly be reported here as well
}

//-----

class C3 {
    class A {
        class B extends A {}
    }
}

class C4 {
    class A extends B {}
    class B {
        class C extends A {}
    }
}
