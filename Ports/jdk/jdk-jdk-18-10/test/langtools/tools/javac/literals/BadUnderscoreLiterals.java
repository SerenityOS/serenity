/*
 * @test /nodynamiccopyright/
 * @bug 6860973
 * @summary Project Coin: underscores in literals
 *
 * @compile/fail/ref=BadUnderscoreLiterals.7.out -XDrawDiagnostics BadUnderscoreLiterals.java
 */

public class BadUnderscoreLiterals {
    int valid = 1_1;            // valid literal

    // test zero
    int z1 = _0;                // valid (but undefined) variable
    int z2 = 0_;                // trailing underscore

    // test simple (decimal) integers
    int i1 = _1_2_3;            // valid (but undefined) variable
    int i2 = 1_2_3_;            // trailing underscore

    // test binary integers
    int b1 = 0b_0;              // leading underscore after radix
    int b2 = 0b0_;              // trailing underscore

    // test hexadecimal integers
    int x1 = 0x_0;              // leading underscore after radix
    int x2 = 0x0_;              // trailing underscore

    // test floating point numbers
    float f1 = 0_.1;            // trailing underscore before decimal point
    float f2 = 0._1;            // leading underscore after decimal point
    float f3 = 0.1_;            // trailing underscore
    float f4 = 0.1_e0;          // trailing underscore before exponent
    float f5 = 0e_1;            // leading underscore in exponent
    float f6 = 0e1_;            // trailing underscore in exponent

    // hexadecimal floating point
    float xf1 = 0x_0.1p0;       // leading underscore after radix
    float xf2 = 0x0_.1p0;       // trailing underscore before decimal point
    float xf3 = 0x0._1p0;       // leading underscore after decimal point
    float xf4 = 0x0.1_p0;       // trailing underscore before exponent
    float xf5 = 0x0p_1;         // leading underscore after exponent
    float xf6 = 0x0p1_;         // trailing underscore
}

