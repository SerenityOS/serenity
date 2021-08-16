/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 4900206
 * @summary Test worst case behavior of exp, log, sin, cos, etc.
 * @build Tests
 * @build WorstCaseTests
 * @run main WorstCaseTests
 * @run main/othervm -Xcomp WorstCaseTests
 * @author Joseph D. Darcy
 */

/**
 * Use "Table Maker's Dilemma" results from Jean-Michel Muller and
 * Vincent Lef&egrave;vre, to test the math library.  See
 * http://perso.ens-lyon.fr/jean-michel.muller/TMD.html for original
 * test vectors from 2000 and see
 * http://perso.ens-lyon.fr/jean-michel.muller/TMDworstcases.pdf with
 * additional test vectors from 2003.  The latter link also contains
 * some information about the methodology used to produce the test
 * vectors.
 *
 * Most of the Java math library methods tested here have a 1-ulp
 * error bound from their specifications.  This implies the returned
 * value must be one of the two representable floating-point numbers
 * bracketing the exact result.  The expected value in the test
 * vectors below is the truncation of the exact value.  Therefore, the
 * computed result must either be that value or the value next larger
 * in magnitude.  The hyperbolic transcendental functions sinh and cosh
 * have a larger 2.5 ulp error bound in their specification, but the
 * JDK implementation complies with a 1 ulp bound on the worst-case
 * values.  Therefore, no addition leeway is afforded when testing
 * sinh and cosh.
 */
public class WorstCaseTests {
    private WorstCaseTests() {throw new AssertionError("No instances for you.");}

    public static void main(String... args) {
        int failures = 0;

        failures += testWorstExp();
        failures += testWorstLog();
        failures += testWorstSin();
        failures += testWorstAsin();
        failures += testWorstCos();
        failures += testWorstAcos();
        failures += testWorstTan();
        failures += testWorstAtan();
        failures += testWorstPow2();
        failures += testWorstSinh();
        failures += testWorstCosh();

        if (failures > 0) {
            System.err.printf("Testing worst cases incurred %d failures.%n", failures);
            throw new RuntimeException();
        }
    }

    private static int testWorstExp() {
        int failures = 0;
        double [][] testCases = {
            {-0x1.E8BDBFCD9144Ep3,      0x1.F3E558CF4DE54p-23},
            {-0x1.71E0B869B5E79p2,      0x1.951C6DC5D24E2p-9},
            {-0x1.02393D5976769p1,      0x1.1064B2C103DDAp-3},
            {-0x1.2A9CAD9998262p0,      0x1.3EF1E9B3A81C7p-2},
            {-0x1.CC37EF7DE7501p0,      0x1.534D4DE870713p-3},
            {-0x1.22E24FA3D5CF9p-1,     0x1.2217147B85EA9p-1},
            {-0x1.DC2B5DF1F7D3Dp-1,     0x1.9403FD0EE51C8p-2},
            {-0x1.290EA09E36479p-3,     0x1.BADED30CBF1C3p-1},
            {-0x1.A2FEFEFD580DFp-13,    0x1.FFE5D0BB7EABFp-1},
            {-0x1.ED318EFB627EAp-27,    0x1.FFFFFF84B39C4p-1},
            {-0x1.4BD46601AE1EFp-31,    0x1.FFFFFFFAD0AE6p-1},
            {-0x1.1000000000242p-42,    0x1.FFFFFFFFFF780p-1},
            {-0x1.2000000000288p-42,    0x1.FFFFFFFFFF700p-1},
            {-0x1.8000000000012p-48,    0x1.FFFFFFFFFFFD0p-1},
            {-0x1.0000000000001p-51,    0x1.FFFFFFFFFFFFCp-1},

            {+0x1.FFFFFFFFFFFFFp-53,    0x1.0000000000000p0},
            {+0x1.FFFFFFFFFFFE0p-48,    0x1.000000000001Fp0},
            {+0x1.7FFE7FFEE0024p-32,    0x1.000000017FFE8p0},
            {+0x1.80017FFEDFFDCp-32,    0x1.0000000180017p0},
            {+0x1.9E9CBBFD6080Bp-31,    0x1.000000033D397p0},
            {+0x1.D7A7D893609E5p-26,    0x1.00000075E9F64p0},
            {+0x1.BA07D73250DE7p-14,    0x1.0006E83736F8Cp0},
            {+0x1.D77FD13D27FFFp-11,    0x1.003AF6C37C1D3p0},
            {+0x1.6A4D1AF9CC989p-8,     0x1.016B4DF3299D7p0},
            {+0x1.ACCFBE46B4EF0p-1,     0x2.4F85C9783DCE0p0},
            {+0x1.ACA7AE8DA5A7Bp0,      0x5.55F52B35F955Ap0},
            {+0x1.D6336A88077AAp0,      0x6.46A37FD503FDCp0},
            {+0x2.85DC78FB8928Cp0,      0xC.76F2496CB038Fp0},
            {+0x1.76E7E5D7B6EACp3,      0x1.DE7CD6751029Ap16},
            {+0x1.A8EAD058BC6B8p3,      0x1.1D71965F516ADp19},
            {+0x1.1D5C2DAEBE367p4,      0x1.A8C02E974C314p25},
            {+0x1.C44CE0D716A1Ap4,      0x1.B890CA8637AE1p40},
        };

        for(double[] testCase: testCases) {
            failures += testExpCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testExpCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.exp",       input, Math.exp(input),       expected, out);
        failures += Tests.testBounds("StrictMath.exp", input, StrictMath.exp(input), expected, out);
        return failures;
    }

    private static int testWorstLog() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.0000000000001p0,      +0x1.FFFFFFFFFFFFFp-53},
            {+0x2.0012ECB039C9Cp0,      +0x1.62F71C4656B60p-1},
            {+0x6.46A37FD503FDCp0,      +0x1.D6336A88077A9p+0},
            {+0x7.78DFECC7F57Fp0,       +0x2.02DD059DB46Bp+0},
            {+0x9.588CCF24BB9C8p0,      +0x2.3C24DEBB2BE7p+0},
            {+0xA.AF87550D97E4p0,       +0x2.5E706595A7ABEp+0},
            {+0xC.76F2496CB039p0,       +0x2.85DC78FB8928Cp+0},
            {+0x11.1867637CBD03p0,      +0x2.D6BBEFC79A842p+0},
            {+0x13.D9D7D597A9DDp0,      +0x2.FCFE12AE07DDCp+0},
            {+0x17.F3825778AAAFp0,      +0x3.2D0F907F5E00Cp+0},
            {+0x1AC.50B409C8AEEp0,      +0x6.0F52F37AECFCCp+0},
            {+0x1.DE7CD6751029Ap16,     +0x1.76E7E5D7B6EABp+3},
        };

        for(double[] testCase: testCases) {
            failures += testLogCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testLogCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.log",       input, Math.log(input),       expected, out);
        failures += Tests.testBounds("StrictMath.log", input, StrictMath.log(input), expected, out);
        return failures;
    }

    private static int testWorstSin() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.E0000000001C2p-20,    +0x1.DFFFFFFFFF02Ep-20},
            {+0x1.598BAE9E632F6p-7,     +0x1.598A0AEA48996p-7},

            {+0x1.9283586503FEp-5,      +0x1.9259E3708BD39p-5},
            {+0x1.D7BDCD778049Fp-5,     +0x1.D77B117F230D5p-5},
            {+0x1.A202B3FB84788p-4,     +0x1.A1490C8C06BA6p-4},
            {+0x1.D037CB27EE6DFp-3,     +0x1.CC40C3805229Ap-3},
            {+0x1.D5064E6FE82C5p-3,     +0x1.D0EF799001BA9p-3},
            {+0x1.FE767739D0F6Dp-2,     +0x1.E9950730C4695p-2},
            {+0x1.D98C4C612718Dp-1,     +0x1.98DCD09337792p-1},
            {+0x1.921FB54442D18p-0,     +0x1.FFFFFFFFFFFFFp-1},

            {+0x1.6756745770A51p+1,     +0x1.4FF350E412821p-2},
        };

        for(double[] testCase: testCases) {
            failures += testSinCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testSinCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.sin",       input, Math.sin(input),       expected, out);
        failures += Tests.testBounds("StrictMath.sin", input, StrictMath.sin(input), expected, out);
        return failures;
    }

    private static int testWorstAsin() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.DFFFFFFFFF02Ep-20,    +0x1.E0000000001C1p-20},
            {+0x1.DFFFFFFFFC0B8p-19,    +0x1.E000000000707p-19},

            {+0x1.9259E3708BD3Ap-5,     +0x1.9283586503FEp-5},
            {+0x1.D77B117F230D6p-5,     +0x1.D7BDCD778049Fp-5},
            {+0x1.A1490C8C06BA7p-4,     +0x1.A202B3FB84788p-4},
            {+0x1.9697CB602C582p-3,     +0x1.994FFB5DAF0F9p-3},
            {+0x1.D0EF799001BA9p-3,     +0x1.D5064E6FE82C4p-3},
            {+0x1.E9950730C4696p-2,     +0x1.FE767739D0F6Dp-2},
            {+0x1.1ED06D50F7E88p-1,     +0x1.30706F699466Dp-1},
            {+0x1.D5B05A89D3E77p-1,     +0x1.29517AB4C132Ap+0},
            {+0x1.E264357EA0E29p-1,     +0x1.3AA301F6EBB1Dp+0},
        };

        for(double[] testCase: testCases) {
            failures += testAsinCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testAsinCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.asin",       input, Math.asin(input),       expected, out);
        failures += Tests.testBounds("StrictMath.asin", input, StrictMath.asin(input), expected, out);
        return failures;
    }

    private static int testWorstCos() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.8000000000009p-23,    +0x0.FFFFFFFFFFFB8p+0},
            {+0x1.8000000000024p-22,    +0x0.FFFFFFFFFFEE0p+0},
            {+0x1.2000000000F30p-18,    +0x0.FFFFFFFFF5E00p+0},
            {+0x1.06B505550E6B2p-9,     +0x0.FFFFDE4D1FDFFp+0},
            {+0x1.97CCD3D2C438Fp-6,     +0x0.FFEBB35D43854p+0},

            {+0x1.549EC0C0C5AFAp-5,     +0x1.FF8EB6A91ECB0p-1},
            {+0x1.16E534EE36580p-4,     +0x1.FED0476FC75C9p-1},
            {+0x1.EFEEF61D39AC2p-3,     +0x1.F10FC61E2C78Ep-1},
            {+0x1.FEB1F7920E248p-2,     +0x1.C1A27AE836F12p-1},
            {+0x1.7CB7648526F99p-1,     +0x1.78DAF01036D0Cp-1},
            {+0x1.C65A170474549p-1,     +0x1.434A3645BE208p-1},
            {+0x1.6B8A6273D7C21p+0,     +0x1.337FC5B072C52p-3},
        };

        for(double[] testCase: testCases) {
            failures += testCosCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testCosCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.cos",       input, Math.cos(input),       expected, out);
        failures += Tests.testBounds("StrictMath.cos", input, StrictMath.cos(input), expected, out);
        return failures;
    }

    private static int testWorstAcos() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.FD737BE914578p-11,    +0x1.91E006D41D8D8p+0},
            {+0x1.4182199998587p-1,     +0x1.C8A538AE83D1Fp-1},
            {+0x1.E45A1C93651ECp-1,     +0x1.520DC553F6B23p-2},
            {+0x1.F10FC61E2C78Fp-1,     +0x1.EFEEF61D39AC1p-3},
        };

        for(double[] testCase: testCases) {
            failures += testAcosCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testAcosCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.acos",       input, Math.acos(input),       expected, out);
        failures += Tests.testBounds("StrictMath.acos", input, StrictMath.acos(input), expected, out);
        return failures;
    }

    private static int testWorstTan() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.DFFFFFFFFFF1Fp-22,    +0x1.E000000000151p-22},
            {+0x1.67FFFFFFFA114p-18,    +0x1.6800000008E61p-18},

            {+0x1.50486B2F87014p-5,     +0x1.5078CEBFF9C72p-5},
            {+0x1.52C39EF070CADp-4,     +0x1.5389E6DF41978p-4},
            {+0x1.A33F32AC5CEB5p-3,     +0x1.A933FE176B375p-3},
            {+0x1.D696BFA988DB9p-2,     +0x1.FAC71CD34EEA6p-2},
            {+0x1.46AC372243536p-1,     +0x1.7BA49F739829Ep-1},
            {+0x0.A3561B9121A9Bp+0,     +0x0.BDD24FB9CC14Fp+0},
        };

        for(double[] testCase: testCases) {
            failures += testTanCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testTanCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.tan",       input, Math.tan(input),       expected, out);
        failures += Tests.testBounds("StrictMath.tan", input, StrictMath.tan(input), expected, out);
        return failures;
    }

    private static int testWorstAtan() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.E000000000546p-21,     +0x1.DFFFFFFFFFC7Cp-21},
            {+0x1.22E8D75E2BC7Fp-11,     +0x1.22E8D5694AD2Bp-11},

            {+0x1.0FC9F1FABE658p-5,     +0x1.0FB06EDE9973Ap-5},
            {+0x1.1BBE9C255698Dp-5,     +0x1.1BA1951DB1D6Dp-5},
            {+0x1.8DDD25AB90CA1p-5,     +0x1.8D8D2D4BD6FA2p-5},
            {+0x1.5389E6DF41979p-4,     +0x1.52C39EF070CADp-4},
            {+0x1.A933FE176B375p-3,     +0x1.A33F32AC5CEB4p-3},
            {+0x1.0F6E5D9960397p-2,     +0x1.09544B71AD4A6p-2},
            {+0x1.7BA49F739829Fp-1,     +0x1.46AC372243536p-1},

            {+0x0.BDD24FB9CC14F8p+0,    +0x0.A3561B9121A9Bp+0},
        };

        for(double[] testCase: testCases) {
            failures += testAtanCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testAtanCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.atan",       input, Math.atan(input),       expected, out);
        failures += Tests.testBounds("StrictMath.atan", input, StrictMath.atan(input), expected, out);
        return failures;
    }

    private static int testWorstPow2() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.16A76EC41B516p-1,     +0x1.7550685A42C63p+0},
            {+0x1.3E34FA6AB969Ep-1,     +0x1.89D948A94FE16p+0},
            {+0x1.4A63FF1D53F53p-1,     +0x1.90661DA12D528p+0},
            {+0x1.B32A6C92D1185p-1,     +0x1.CD6B37EDECEAFp+0},

            {+0x1.25DD9EEDAC79Ap+0,     +0x1.1BA39FF28E3E9p+1},
        };

        for(double[] testCase: testCases) {
            failures += testPow2Case(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testPow2Case(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.pow2",       input, Math.pow(2, input),       expected, out);
        failures += Tests.testBounds("StrictMath.pow2", input, StrictMath.pow(2, input), expected, out);
        return failures;
    }

    // 2.5 ulp error bound in the specification; the implementation
    // does better on the tested values.
    private static int testWorstSinh() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.DFFFFFFFFFE3Ep-20,     +0x1.E000000000FD1p-20},
            {+0x1.DFFFFFFFFE3E0p-18,     +0x1.E00000000FD1Fp-18},
            {+0x1.135E31FDD05D3p-5,      +0x1.136B78B25CC57p-5},
            {+0x1.0DC68D5E8F959p-3,      +0x1.0E8E73DC4FEE3p-3},
            {+0x1.616CC75D49226p-2,      +0x1.687BD068C1C1Ep-2},
            {+0x1.3FFC12B81CBC2p+0,      +0x1.9A0FF413A1AF2p+0},
            {+0x2.FE008C44BACA2p+0,      +0x9.F08A43ED03AEp+0},
            {+0x1.C089FCF166171p+4,      +0x1.5C452E0E37569p+39},
            {+0x1.E07E71BFCF06Fp+5,      +0x1.91EC4412C344Fp+85},
            {+0x1.54CD1FEA7663Ap+7,      +0x1.C90810D354618p+244},
            {+0x1.D6479EBA7C971p+8,      +0x1.62A88613629B5p+677},
        };

        for(double[] testCase: testCases) {
            failures += testSinhCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testSinhCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.sinh",       input, Math.sinh(input),       expected, out);
        failures += Tests.testBounds("StrictMath.sinh", input, StrictMath.sinh(input), expected, out);
        return failures;
    }

    // 2.5 ulp error bound in the specification; the implementation
    // does better on the tested values.
    private static int testWorstCosh() {
        int failures = 0;
        double [][] testCases = {
            {+0x1.17D8A9F206217p-6,     +0x1.00098F5F09BE3p+0},
            {+0x1.BF0305E2C6C37p-3,     +0x1.061F4C39E16F2p+0},
            {+0x1.03923F2B47C07p-1,     +0x1.219C1989E3372p+0},
            {+0x1.A6031CD5F93BAp-1,     +0x1.5BFF041B260FDp+0},
            {+0x1.104B648F113A1p+0,     +0x1.9EFDCA62B7009p+0},
            {+0x1.EA5F2F2E4B0C5p+1,     +0x17.10DB0CD0FED5p+0},
        };

        for(double[] testCase: testCases) {
            failures += testCoshCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testCoshCase(double input, double expected) {
        int failures = 0;
        double out = Tests.nextOut(expected);
        failures += Tests.testBounds("Math.cosh",       input, Math.cosh(input),       expected, out);
        failures += Tests.testBounds("StrictMath.cosh", input, StrictMath.cosh(input), expected, out);
        return failures;
    }
}
