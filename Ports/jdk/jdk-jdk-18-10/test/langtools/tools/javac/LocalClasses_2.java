/*
 * @test  /nodynamiccopyright/
 * @bug 4074421 4277278 4785453
 * @summary Verify that a local class cannot be redefined within its scope.
 * @author William Maddox (maddox)
 *
 * @compile/fail/ref=LocalClasses_2.out -XDrawDiagnostics LocalClasses_2.java
 */

class LocalClasses_2 {

    void foo() {
        class Local { }
        {
            class Local { }                     // ERROR
        }
    }

    void bar() {

        class Local { }

        class Baz {
            void quux() {
                class Local { }                 // OK
            }
        }

        class Quux {
            void baz() {
                class Random {
                    void quem() {
                        class Local { }         // OK
                    }
                }
            }
        }
    }
}
