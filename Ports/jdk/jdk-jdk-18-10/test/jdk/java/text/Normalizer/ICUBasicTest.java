/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug  4221795 8032446 8174270
 * @summary Confirm Normalizer's fundamental behavior. Imported from ICU4J 3.2's
 * src/com/ibm/icu/dev/test and modified.
 * @modules java.base/sun.text java.base/jdk.internal.icu.text
 * @library /java/text/testlib
 * @compile -XDignore.symbol.file ICUBasicTest.java
 * @run main/timeout=30 ICUBasicTest
 */

/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

import sun.text.Normalizer;
import jdk.internal.icu.text.NormalizerBase;

import static java.text.Normalizer.Form.*;

public class ICUBasicTest extends IntlTest {

    public static void main(String[] args) throws Exception {
        new ICUBasicTest().run(args);
    }

    /*
     * Normalization modes
     */
    private static final NormalizerBase.Mode NFCmode  = NormalizerBase.NFC;
    private static final NormalizerBase.Mode NFDmode  = NormalizerBase.NFD;
    private static final NormalizerBase.Mode NFKCmode = NormalizerBase.NFKC;
    private static final NormalizerBase.Mode NFKDmode = NormalizerBase.NFKD;
    private static final NormalizerBase.Mode NONEmode = NormalizerBase.NONE;

    /*
     * Normalization options
     */

    /* Normal Unicode versions */
    private static final int UNICODE_3_2_0  = Normalizer.UNICODE_3_2;
    private static final int UNICODE_LATEST = NormalizerBase.UNICODE_LATEST;

    /*
     * Special cases for UAX #15 bug
     * see Unicode Public Review Issue #29
     * at http://www.unicode.org/review/resolved-pri.html#pri29
     *
     * Note:
     *   PRI #29 is supported in Unicode 4.1.0. Therefore, expected results are
     *   different for earlier Unicode versions.
     */
    public void TestComposition() {

        final TestCompositionCase cases[] = new TestCompositionCase[] {
            new TestCompositionCase(NFC, UNICODE_3_2_0,
                "\u1100\u0300\u1161\u0327",
                "\u1100\u0300\u1161\u0327"),
            new TestCompositionCase(NFC, UNICODE_LATEST,
                "\u1100\u0300\u1161\u0327",
                "\u1100\u0300\u1161\u0327"),

            new TestCompositionCase(NFC, UNICODE_3_2_0,
                "\u1100\u0300\u1161\u0327\u11a8",
                "\u1100\u0300\u1161\u0327\u11a8"),
            new TestCompositionCase(NFC, UNICODE_LATEST,
                "\u1100\u0300\u1161\u0327\u11a8",
                "\u1100\u0300\u1161\u0327\u11a8"),

            new TestCompositionCase(NFC, UNICODE_3_2_0,
                "\uac00\u0300\u0327\u11a8",
                "\uac00\u0327\u0300\u11a8"),
            new TestCompositionCase(NFC, UNICODE_LATEST,
                "\uac00\u0300\u0327\u11a8",
                "\uac00\u0327\u0300\u11a8"),

            new TestCompositionCase(NFC, UNICODE_3_2_0,
                "\u0b47\u0300\u0b3e",
                "\u0b47\u0300\u0b3e"),
            new TestCompositionCase(NFC, UNICODE_LATEST,
                "\u0b47\u0300\u0b3e",
                "\u0b47\u0300\u0b3e"),
        };

        String output;
        int i, length;

        for (i=0; i<cases.length; ++i) {
            output = Normalizer.normalize(cases[i].input,
                                          cases[i].form, cases[i].options);
            if (!output.equals(cases[i].expect)) {
                errln("unexpected result for case " + i + ". Expected="
                      + cases[i].expect + ", Actual=" + output);
            } else if (verbose) {
                logln("expected result for case " + i + ". Expected="
                      + cases[i].expect + ", Actual=" + output);
            }
        }
    }

    private final static class TestCompositionCase {
        public java.text.Normalizer.Form form;
        public int options;
        public String input, expect;

        TestCompositionCase(java.text.Normalizer.Form form,
                            int options,
                            String input,
                            String expect) {
            this.form    = form;
            this.options = options;
            this.input   = input;
            this.expect  = expect;
        }
    }

    /*
     * Added in order to detect a regression.
     */
    public void TestCombiningMarks() {
        String src      = "\u0f71\u0f72\u0f73\u0f74\u0f75";
        String expected = "\u0F71\u0F71\u0F71\u0F72\u0F72\u0F74\u0F74";
        String result   = NormalizerBase.normalize(src, NFD);

        if (!expected.equals(result)) {
            errln("Reordering of combining marks failed. Expected: " +
                  toHexString(expected) + " Got: "+ toHexString(result));
        }
    }

    /*
     * Added in order to detect a regression.
     */
    public void TestBengali() throws Exception {
        String input = "\u09bc\u09be\u09cd\u09be";
        String output=NormalizerBase.normalize(input, NFC);

        if (!input.equals(output)) {
             errln("ERROR in NFC of string");
        }
        return;
    }


    /*
     * Added in order to detect a regression.
     */
    /**
     * Test for a problem found by Verisign.  Problem is that
     * characters at the start of a string are not put in canonical
     * order correctly by compose() if there is no starter.
     */
    public void TestVerisign() throws Exception {
        String[] inputs = {
            "\u05b8\u05b9\u05b1\u0591\u05c3\u05b0\u05ac\u059f",
            "\u0592\u05b7\u05bc\u05a5\u05b0\u05c0\u05c4\u05ad"
        };
        String[] outputs = {
            "\u05b1\u05b8\u05b9\u0591\u05c3\u05b0\u05ac\u059f",
            "\u05b0\u05b7\u05bc\u05a5\u0592\u05c0\u05ad\u05c4"
        };

        for (int i = 0; i < inputs.length; ++i) {
            String input = inputs[i];
            String output = outputs[i];

            String result = NormalizerBase.normalize(input, NFD);
            if (!result.equals(output)) {
                errln("FAIL input: " + toHexString(input) + "\n" +
                      " decompose: " + toHexString(result) + "\n" +
                      "  expected: " + toHexString(output));
            }

            result = NormalizerBase.normalize(input, NFC);
            if (!result.equals(output)) {
                errln("FAIL input: " + toHexString(input) + "\n" +
                      "   compose: " + toHexString(result) + "\n" +
                      "  expected: " + toHexString(output));
            }
        }
    }

    /**
     * Test for a problem that showed up just before ICU 1.6 release
     * having to do with combining characters with an index of zero.
     * Such characters do not participate in any canonical
     * decompositions.  However, having an index of zero means that
     * they all share one typeMask[] entry, that is, they all have to
     * map to the same canonical class, which is not the case, in
     * reality.
     */
    public void TestZeroIndex() throws Exception {
        String[] DATA = {
            // Expect col1 x COMPOSE_COMPAT => col2
            // Expect col2 x DECOMP => col3
            "A\u0316\u0300", "\u00C0\u0316", "A\u0316\u0300",
            "A\u0300\u0316", "\u00C0\u0316", "A\u0316\u0300",
            "A\u0327\u0300", "\u00C0\u0327", "A\u0327\u0300",
            "c\u0321\u0327", "c\u0321\u0327", "c\u0321\u0327",
            "c\u0327\u0321", "\u00E7\u0321", "c\u0327\u0321",
        };

        for (int i=0; i<DATA.length; i+=3) {
            String a = DATA[i];
            String b = NormalizerBase.normalize(a, NFKC);
            String exp = DATA[i+1];

            if (b.equals(exp)) {
                logln("Ok: " + toHexString(a) + " x COMPOSE_COMPAT => " +
                      toHexString(b));
            } else {
                errln("FAIL: " + toHexString(a) + " x COMPOSE_COMPAT => " +
                      toHexString(b) + ", expect " + toHexString(exp));
            }

            a = NormalizerBase.normalize(b, NFD);
            exp = DATA[i+2];
            if (a.equals(exp)) {
                logln("Ok: " + toHexString(b) + " x DECOMP => " +
                      toHexString(a));
            } else {
                errln("FAIL: " + toHexString(b) + " x DECOMP => " +
                      toHexString(a) + ", expect " + toHexString(exp));
            }
        }
    }

    /**
     * Make sure characters in the CompositionExclusion.txt list do not get
     * composed to.
     */
    public void TestCompositionExclusion() throws Exception {
        // This list is generated from CompositionExclusion.txt.
        // Update whenever the normalizer tables are updated.  Note
        // that we test all characters listed, even those that can be
        // derived from the Unicode DB and are therefore commented
        // out.

        /*
         * kyuka's note:
         *   Original data seemed to be based on Unicode 3.0.0(the initial
         *   Composition Exclusions list) and seemed to have some mistakes.
         *   Updated in order to correct mistakes and to support Unicode 4.0.0.
         *   And, this table can be used also for Unicode 3.2.0.
         */
        String[][] EXCLUDED_UNICODE_3_2_0 = {
            {"\u0340"},
            {"\u0341"},
            {"\u0343"},
            {"\u0344"},
            {"\u0374"},
            {"\u037E"},
            {"\u0387"},
            {"\u0958"},
            {"\u0959", "\u095F"},
            {"\u09DC"},
            {"\u09DD"},
            {"\u09DF"},
            {"\u0A33"},
            {"\u0A36"},
            {"\u0A59", "\u0A5B"},
            {"\u0A5E"},
            {"\u0B5C"},
            {"\u0B5D"},
            {"\u0F43"},
            {"\u0F4D"},
            {"\u0F52"},
            {"\u0F57"},
            {"\u0F5C"},
            {"\u0F69"},
            {"\u0F73"},
            {"\u0F75"},
            {"\u0F76"},
            {"\u0F78"},
            {"\u0F81"},
            {"\u0F93"},
            {"\u0F9D"},
            {"\u0FA2"},
            {"\u0FA7"},
            {"\u0FAC"},
            {"\u0FB9"},
            {"\u1F71"},
            {"\u1F73"},
            {"\u1F75"},
            {"\u1F77"},
            {"\u1F79"},
            {"\u1F7B"},
            {"\u1F7D"},
            {"\u1FBB"},
            {"\u1FBE"},
            {"\u1FC9"},
            {"\u1FCB"},
            {"\u1FD3"},
            {"\u1FDB"},
            {"\u1FE3"},
            {"\u1FEB"},
            {"\u1FEE"},
            {"\u1FEF"},
            {"\u1FF9"},
            {"\u1FFB"},
            {"\u1FFD"},
            {"\u2000"},
            {"\u2001"},
            {"\u2126"},
            {"\u212A"},
            {"\u212B"},
            {"\u2329"},
            {"\u232A"},
            {"\u2ADC"},
            {"\uF900", "\uFA0D"},
            {"\uFA10"},
            {"\uFA12"},
            {"\uFA15", "\uFA1E"},
            {"\uFA20"},
            {"\uFA22"},
            {"\uFA25"},
            {"\uFA26"},
            {"\uFA2A", "\uFA2D"},
            {"\uFA30", "\uFA6A"},
            {"\uFB1D"},
            {"\uFB1F"},
            {"\uFB2A", "\uFB36"},
            {"\uFB38", "\uFB3C"},
            {"\uFB3E"},
            {"\uFB40"},
            {"\uFB41"},
            {"\uFB43"},
            {"\uFB44"},
            {"\uFB46", "\uFB4E"},
            {"\uD834\uDD5E", "\uD834\uDD64"},
            {"\uD834\uDDBB", "\uD834\uDDC0"},
            {"\uD87E\uDC00", "\uD87E\uDE1D"}
        };

        String[][] EXCLUDED_LATEST = {

        };

        for (int i = 0; i < EXCLUDED_UNICODE_3_2_0.length; ++i) {
            if (EXCLUDED_UNICODE_3_2_0[i].length == 1) {
                checkCompositionExclusion_320(EXCLUDED_UNICODE_3_2_0[i][0]);
            } else {
                int from, to;
                from = Character.codePointAt(EXCLUDED_UNICODE_3_2_0[i][0], 0);
                to   = Character.codePointAt(EXCLUDED_UNICODE_3_2_0[i][1], 0);

                for (int j = from; j <= to; j++) {
                    checkCompositionExclusion_320(String.valueOf(Character.toChars(j)));
                }
            }
        }
    }

    private void checkCompositionExclusion_320(String s) throws Exception {
        String a = String.valueOf(s);
        String b = NormalizerBase.normalize(a, NFKD);
        String c = NormalizerBase.normalize(b, NFC);

        if (c.equals(a)) {
            errln("FAIL: " + toHexString(a) + " x DECOMP_COMPAT => " +
                  toHexString(b) + " x COMPOSE => " +
                  toHexString(c) + " for the latest Unicode");
        } else if (verbose) {
            logln("Ok: " + toHexString(a) + " x DECOMP_COMPAT => " +
                  toHexString(b) + " x COMPOSE => " +
                  toHexString(c) + " for the latest Unicode");
        }

        b = NormalizerBase.normalize(a, NFKD, Normalizer.UNICODE_3_2);
        c = NormalizerBase.normalize(b, NFC, Normalizer.UNICODE_3_2);
        if (c.equals(a)) {
            errln("FAIL: " + toHexString(a) + " x DECOMP_COMPAT => " +
                  toHexString(b) + " x COMPOSE => " +
                  toHexString(c) + " for Unicode 3.2.0");
        } else if (verbose) {
            logln("Ok: " + toHexString(a) + " x DECOMP_COMPAT => " +
                  toHexString(b) + " x COMPOSE => " +
                  toHexString(c) + " for Unicode 3.2.0");
        }
    }

    public void TestTibetan() throws Exception {
        String[][] decomp = {
            { "\u0f77", "\u0f77", "\u0fb2\u0f71\u0f80" }
        };
        String[][] compose = {
            { "\u0fb2\u0f71\u0f80", "\u0fb2\u0f71\u0f80", "\u0fb2\u0f71\u0f80" }
        };

        staticTest(NFD, decomp, 1);
        staticTest(NFKD,decomp, 2);
        staticTest(NFC, compose, 1);
        staticTest(NFKC,compose, 2);
    }

    public void TestExplodingBase() throws Exception{
        // \u017f - Latin small letter long s
        // \u0307 - combining dot above
        // \u1e61 - Latin small letter s with dot above
        // \u1e9b - Latin small letter long s with dot above
        String[][] canon = {
            // Input                Decomposed              Composed
            { "Tschu\u017f",        "Tschu\u017f",          "Tschu\u017f"    },
            { "Tschu\u1e9b",        "Tschu\u017f\u0307",    "Tschu\u1e9b"    },
        };
        String[][] compat = {
            // Input                Decomposed              Composed
            { "\u017f",             "s",                    "s"           },
            { "\u1e9b",             "s\u0307",              "\u1e61"      },
        };

        staticTest(NFD, canon,  1);
        staticTest(NFC, canon,  2);
        staticTest(NFKD, compat, 1);
        staticTest(NFKC, compat, 2);
    }

    private String[][] canonTests = {
        // Input                Decomposed              Composed

        { "cat",                "cat",                  "cat"               },
        { "\u00e0ardvark",      "a\u0300ardvark",       "\u00e0ardvark",    },

        // D-dot_above
        { "\u1e0a",             "D\u0307",              "\u1e0a"            },

        // D dot_above
        { "D\u0307",            "D\u0307",              "\u1e0a"            },

        // D-dot_below dot_above
        { "\u1e0c\u0307",       "D\u0323\u0307",        "\u1e0c\u0307"      },

        // D-dot_above dot_below
        { "\u1e0a\u0323",       "D\u0323\u0307",        "\u1e0c\u0307"      },

        // D dot_below dot_above
        { "D\u0307\u0323",      "D\u0323\u0307",        "\u1e0c\u0307"      },

        // D dot_below cedilla dot_above
        { "\u1e10\u0307\u0323", "D\u0327\u0323\u0307",  "\u1e10\u0323\u0307"},

        // D dot_above ogonek dot_below
        { "D\u0307\u0328\u0323","D\u0328\u0323\u0307",  "\u1e0c\u0328\u0307"},

        // E-macron-grave
        { "\u1E14",             "E\u0304\u0300",        "\u1E14"            },

        // E-macron + grave
        { "\u0112\u0300",       "E\u0304\u0300",        "\u1E14"            },

        // E-grave + macron
        { "\u00c8\u0304",       "E\u0300\u0304",        "\u00c8\u0304"      },

        // angstrom_sign
        { "\u212b",             "A\u030a",              "\u00c5"            },

        // A-ring
        { "\u00c5",             "A\u030a",              "\u00c5"            },
        { "\u00c4ffin",         "A\u0308ffin",          "\u00c4ffin"        },
        { "\u00c4\uFB03n",      "A\u0308\uFB03n",       "\u00c4\uFB03n"     },

        //updated with 3.0
        { "\u00fdffin",         "y\u0301ffin",          "\u00fdffin"        },
        { "\u00fd\uFB03n",      "y\u0301\uFB03n",       "\u00fd\uFB03n"     },

        { "Henry IV",           "Henry IV",             "Henry IV"          },
        { "Henry \u2163",       "Henry \u2163",         "Henry \u2163"      },

        // ga(Zenkaku-Katakana)
        { "\u30AC",             "\u30AB\u3099",         "\u30AC"            },

        // ka(Zenkaku-Katakana) + ten(Zenkaku)
        { "\u30AB\u3099",       "\u30AB\u3099",         "\u30AC"            },

        // ka(Hankaku-Katakana) + ten(Hankaku-Katakana)
        { "\uFF76\uFF9E",       "\uFF76\uFF9E",         "\uFF76\uFF9E"      },

        // ka(Zenkaku-Katakana) + ten(Hankaku)
        { "\u30AB\uFF9E",       "\u30AB\uFF9E",         "\u30AB\uFF9E"      },
        // ka(Hankaku-Katakana) + ten(Zenkaku)
        { "\uFF76\u3099",       "\uFF76\u3099",         "\uFF76\u3099"      },

        { "A\u0300\u0316", "A\u0316\u0300", "\u00C0\u0316" },

        { "\ud834\udd5e\ud834\udd57\ud834\udd65\ud834\udd5e",
          "\ud834\udd57\ud834\udd65\ud834\udd57\ud834\udd65\ud834\udd57\ud834\udd65",
          "\ud834\udd57\ud834\udd65\ud834\udd57\ud834\udd65\ud834\udd57\ud834\udd65" },
    };

    private String[][] compatTests = {
        // Input                Decomposed              Composed

        { "cat",                 "cat",                     "cat"           },

        // Alef-Lamed vs. Alef, Lamed
        { "\uFB4f",             "\u05D0\u05DC",         "\u05D0\u05DC",     },

        { "\u00C4ffin",         "A\u0308ffin",          "\u00C4ffin"        },

        // ffi ligature -> f + f + i
        { "\u00C4\uFB03n",      "A\u0308ffin",          "\u00C4ffin"        },

        //updated for 3.0
        { "\u00fdffin",         "y\u0301ffin",          "\u00fdffin"        },

        // ffi ligature -> f + f + i
        { "\u00fd\uFB03n",      "y\u0301ffin",          "\u00fdffin"        },

        { "Henry IV",           "Henry IV",             "Henry IV"          },
        { "Henry \u2163",       "Henry IV",             "Henry IV"          },

        // ga(Zenkaku-Katakana)
        { "\u30AC",             "\u30AB\u3099",         "\u30AC"            },

        // ka(Zenkaku-Katakana) + ten(Zenkaku)
        { "\u30AB\u3099",       "\u30AB\u3099",         "\u30AC"            },

        // ka(Hankaku-Katakana) + ten(Zenkaku)
        { "\uFF76\u3099",       "\u30AB\u3099",         "\u30AC"            },

        /* These two are broken in Unicode 2.1.2 but fixed in 2.1.5 and later*/
        // ka(Hankaku-Katakana) + ten(Hankaku)
        { "\uFF76\uFF9E",       "\u30AB\u3099",         "\u30AC"            },

        // ka(Zenkaku-Katakana) + ten(Hankaku)
        { "\u30AB\uFF9E",       "\u30AB\u3099",         "\u30AC"            },
    };

    public void TestNFD() throws Exception{
        staticTest(NFD, canonTests, 1);
    }

    public void TestNFC() throws Exception{
        staticTest(NFC, canonTests, 2);
    }

    public void TestNFKD() throws Exception{
        staticTest(NFKD, compatTests, 1);
    }

    public void TestNFKC() throws Exception{
        staticTest(NFKC, compatTests, 2);
    }

    private void staticTest(java.text.Normalizer.Form form,
                            String[][] tests,
                            int outCol) throws Exception {
        for (int i = 0; i < tests.length; i++) {
            String input = tests[i][0];
            logln("Normalizing '" + input + "' (" + toHexString(input) + ")" );

            String expect =tests[i][outCol];
            String output = java.text.Normalizer.normalize(input, form);

            if (!output.equals(expect)) {
                errln("FAIL: case " + i
                    + " expected '" + expect + "' (" + toHexString(expect) + ")"
                    + " but got '" + output + "' (" + toHexString(output) + ")"
);
            }
        }
    }

    // With Canonical decomposition, Hangul syllables should get decomposed
    // into Jamo, but Jamo characters should not be decomposed into
    // conjoining Jamo
    private String[][] hangulCanon = {
        // Input                Decomposed              Composed
        { "\ud4db",             "\u1111\u1171\u11b6",   "\ud4db"        },
        { "\u1111\u1171\u11b6", "\u1111\u1171\u11b6",   "\ud4db"        },
    };

    public void TestHangulCompose() throws Exception{
        logln("Canonical composition...");
        staticTest(NFC, hangulCanon,  2);
     }

    public void TestHangulDecomp() throws Exception{
        logln("Canonical decomposition...");
        staticTest(NFD, hangulCanon, 1);
    }

}
