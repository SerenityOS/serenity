/*
 * @test  /nodynamiccopyright/
 * @bug 4095568 4277286 4785453
 * @summary Verify rejection of illegal static variables in inner classes.
 * @author William Maddox (maddox)
 *
 * @compile/fail/ref=InnerNamedConstant_2_A.out -XDrawDiagnostics -source 15 InnerNamedConstant_2.java
 * @compile/fail/ref=InnerNamedConstant_2_B.out -XDrawDiagnostics InnerNamedConstant_2.java
 */

public class InnerNamedConstant_2 {

    static class Inner1 {
        static int x = 1;                  // OK - class is top-level
        static final int y = x * 5;        // OK - class is top-level
        static final String z;             // OK - class is top-level
        static {
            z = "foobar";
        }
    }

    class Inner2 {
        static int x = 1;                  // ERROR - static not final
        static final String z;             // ERROR - static blank final
        {
            z = "foobar";                  // Error may be reported here. See 4278961.
        }
    }

    // This case must go in a separate class, as otherwise the detection
    // of the error is suppressed as a result of recovery from the other
    // errors.

    class Inner3 {
        static final int y = Inner1.x * 5; // ERROR - initializer not constant
    }

}
