/*
 * @test  /nodynamiccopyright/
 * @bug 4037746 4277279 4350658 4785453
 * @summary Verify that an inner class cannot have the same simple name as an enclosing one.
 * @author William Maddox (maddox)
 *
 * @compile/fail/ref=NestedInnerClassNames.out -XDrawDiagnostics NestedInnerClassNames.java
 */

/*
 * This program should compile with errors as marked.
 */

public class NestedInnerClassNames {

    class NestedInnerClassNames {}              // ERROR

    void m1() {
        class NestedInnerClassNames {}          // ERROR
    }

    class foo {
        class foo { }                           // ERROR
    }

    void m2 () {
        class foo {
            class foo { }                       // ERROR
        }
    }

    class bar {
        class foo { }
        class NestedInnerClassNames {}          // ERROR
    }

    void m3() {
        class bar {
            class foo { }
            class NestedInnerClassNames {}      // ERROR
        }
    }

    class baz {
        class baz {                             // ERROR
            class baz { }                       // ERROR
        }
    }

    void m4() {
        class baz {
            class baz {                         // ERROR
                class baz { }                   // ERROR
            }
        }
    }

    class foo$bar {
        class foo$bar {                         // ERROR
            class foo { }
            class bar { }
        }
    }

    void m5() {
        class foo$bar {
            class foo$bar {                     // ERROR
                class foo { }
                class bar { }
            }
        }
    }

    class $bar {
        class foo {
            class $bar { }                      // ERROR
        }
    }

    void m6() {
        class $bar {
            class foo {
                class $bar { }                  // ERROR
            }
        }
    }

    class bar$bar {
        class bar {
            class bar{ }                       // ERROR
        }
    }

    void m7() {
        class bar$bar {
            class bar {
                class bar{ }                   // ERROR
            }
        }
    }

    // The name of the class below clashes with the name of the
    // class created above for 'class foo { class foo {} }'.
    // The clash follows from the naming requirements of the inner
    // classes spec, but is most likely a specification bug.

    // Error may be reported here.  See 4278961.
    // As of Merlin-beta b21, this now results in an error.
    class foo$foo { }                           // ERROR

}
