/**
 * @test  /nodynamiccopyright/
 * @bug 4094658 4277296 4785453
 * @summary Test enforcement of JLS 6.6.1 and 6.6.2 rules requiring that
 * the type to which a component member belongs be accessible in qualified
 * names.
 *
 * @compile pack1/P1.java
 * @compile pack1/P2.java
 * @compile/fail/ref=QualifiedAccess_1.out -XDrawDiagnostics QualifiedAccess_1.java
 */

import pack1.P1;

public class QualifiedAccess_1 {

    // Inaccessible types in member declarations.
    // These exercise 'Env.resolve'.
    // Errors are localized poorly.
    //
    // Fields 'P3' and 'P5' are inaccessible.

    P1 foo;
    P1.P3 bar;                                  // ERROR
    P1.P3.P4 baz;                               // ERROR
    P1.P3.P4.P5 quux;                           // ERROR

    P1 m11() {return null;}
    P1.P3 m12() {return null;}                  // ERROR
    P1.P3.P4 m13() {return null;}               // ERROR
    P1.P3.P4.P5 m14() {return null;}            // ERROR

    void m21(P1 x) {}
    void m22(P1.P3 x) {}                        // ERROR
    void m23(P1.P3.P4 x) {}                     // ERROR
    void m24(P1.P3.P4.P5 x) {}                  // ERROR

    void test1() {

        // Inaccessible types in local variable declarations.
        // These exercise 'FieldExpression.checkCommon'.
        //
        // Fields 'P3' and 'P5' are inaccessible.

        P1 foo = null;
        P1.P3 bar = null;                       // ERROR
        P1.P3.P4 baz = null;                    // ERROR
        P1.P3.P4.P5 quux = null;                // ERROR
    }

    void test2() {

        // Inaccessible types in casts.
        // These exercise 'FieldExpression.checkCommon'.
        //
        // Fields 'P3' and 'P5' are inaccessible.

        Object foo = (P1)null;
        Object bar = (P1.P3)null;               // ERROR
        Object baz = (P1.P3.P4)null;            // ERROR
        Object quux = (P1.P3.P4.P5)null;        // ERROR
    }

    void test3() {

        // Inaccessible types in 'instanceof' expressions.
        // These exercise 'FieldExpression.checkCommon'.
        //
        // Fields 'P3' and 'P5' are inaccessible.

        boolean foo = null instanceof P1;
        boolean bar = null instanceof P1.P3;            // ERROR
        boolean baz = null instanceof P1.P3.P4;         // ERROR
        boolean quux = null instanceof P1.P3.P4.P5;     // ERROR
    }

}
