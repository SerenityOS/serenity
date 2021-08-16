/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4328178
 * @summary Performs baseline and regression test on the ISCII91 charset
 * @modules jdk.charsets
 */

import java.io.*;

public class ISCIITest {

    private static void failureReport() {
        System.err.println ("Failed ISCII91 Regression Test");
    }

    private static void mapEquiv(int start,
                                 int end,
                                 String testName)
    throws Exception
    {
        byte[] singleByte = new byte[1];
        byte[] encoded = new byte[1];

        for (int i = start; i <= end; i++ ) {
            singleByte[0] = (byte) i;
            try {
                String unicodeStr =
                        new String (singleByte,"ISCII91");

                if (i != (int)unicodeStr.charAt(0)) {
                    System.err.println ("FAILED ISCII91 Regression test"
                                        + "input byte is " + i );
                    throw new Exception("");
                }
                encoded = unicodeStr.getBytes("ISCII91");

                if (encoded[0] != singleByte[0]) {
                   System.err.println("Encoding error " + testName);
                   throw new Exception("Failed ISCII91 Regression test");
                }

            } catch (UnsupportedEncodingException e) {
                failureReport();
            }
        }
        return;
    }

    private static void checkUnmapped(int start,
                                      int end,
                                      String testName)
    throws Exception {

        byte[] singleByte = new byte[1];

        for (int i = start; i <= end; i++ ) {
            singleByte[0] = (byte) i;
            try {
                String unicodeStr = new String (singleByte, "ISCII91");

                if (unicodeStr.charAt(0) != '\uFFFD') {
                    System.err.println("FAILED " + testName +
                                        "input byte is " + i );
                    throw new Exception ("Failed ISCII91 regression test");
                }
            } catch (UnsupportedEncodingException e) {
                System.err.println("Unsupported character encoding");
            }
        }
        return;
    }

    /*
     *
     */
    private static void checkRange(int start, int end,
                                   char[] expectChars,
                                   String testName)
                                   throws Exception {
        byte[] singleByte = new byte[1];
        byte[] encoded = new byte[1];
        int lookupOffset = 0;

        for (int i=start; i <= end; i++ ) {
            singleByte[0] = (byte) i;
            String unicodeStr = new String (singleByte, "ISCII91");
            if (unicodeStr.charAt(0) != expectChars[lookupOffset++]) {
                throw new Exception ("Failed ISCII91 Regression Test");
            }
            encoded = unicodeStr.getBytes("ISCII");
        }
        return;
    }

    /*
     * Tests the ISCII91 Indic character encoding
     * as per IS 13194:1991 Bureau of Indian Standards.
     */

    private static void test () throws Exception {

        try {


            // ISCII91 is an 8-byte encoding which retains the ASCII
            // mappings in the lower half.

            mapEquiv(0, 0x7f, "7 bit ASCII range");

            // Checks a range of characters which are unmappable according
            // to the standards.

            checkUnmapped(0x81, 0x9f, "UNMAPPED");

            // Vowel Modifier chars can be used to modify the vowel
            // sound of the preceding consonant, vowel or matra character.

            byte[] testByte = new byte[1];
            char[] vowelModChars = {
                '\u0901', // Vowel modifier Chandrabindu
                '\u0902', // Vowel modifier Anuswar
                '\u0903'  // Vowel modifier Visarg
            };

            checkRange(0xa1, 0xa3, vowelModChars, "INDIC VOWEL MODIFIER CHARS");

            char[] expectChars = {
                '\u0905', // a4 -- Vowel A
                '\u0906', // a5 -- Vowel AA
                '\u0907', // a6 -- Vowel I
                '\u0908', // a7 -- Vowel II
                '\u0909', // a8 -- Vowel U
                '\u090a', // a9 -- Vowel UU
                '\u090b', // aa -- Vowel RI
                '\u090e', // ab -- Vowel E ( Southern Scripts )
                '\u090f', // ac -- Vowel EY
                '\u0910', // ad -- Vowel AI
                '\u090d', // ae -- Vowel AYE ( Devanagari Script )
                '\u0912', // af -- Vowel O ( Southern Scripts )
                '\u0913', // b0 -- Vowel OW
                '\u0914', // b1 -- Vowel AU
                '\u0911', // b2 -- Vowel AWE ( Devanagari Script )
            };

            checkRange(0xa4, 0xb2, expectChars, "INDIC VOWELS");

            char[] expectConsChars =
            {
                '\u0915', // b3 -- Consonant KA
                '\u0916', // b4 -- Consonant KHA
                '\u0917', // b5 -- Consonant GA
                '\u0918', // b6 -- Consonant GHA
                '\u0919', // b7 -- Consonant NGA
                '\u091a', // b8 -- Consonant CHA
                '\u091b', // b9 -- Consonant CHHA
                '\u091c', // ba -- Consonant JA
                '\u091d', // bb -- Consonant JHA
                '\u091e', // bc -- Consonant JNA
                '\u091f', // bd -- Consonant Hard TA
                '\u0920', // be -- Consonant Hard THA
                '\u0921', // bf -- Consonant Hard DA
                '\u0922', // c0 -- Consonant Hard DHA
                '\u0923', // c1 -- Consonant Hard NA
                '\u0924', // c2 -- Consonant Soft TA
                '\u0925', // c3 -- Consonant Soft THA
                '\u0926', // c4 -- Consonant Soft DA
                '\u0927', // c5 -- Consonant Soft DHA
                '\u0928', // c6 -- Consonant Soft NA
                '\u0929', // c7 -- Consonant NA ( Tamil )
                '\u092a', // c8 -- Consonant PA
                '\u092b', // c9 -- Consonant PHA
                '\u092c', // ca -- Consonant BA
                '\u092d', // cb -- Consonant BHA
                '\u092e', // cc -- Consonant MA
                '\u092f', // cd -- Consonant YA
                '\u095f', // ce -- Consonant JYA ( Bengali, Assamese & Oriya )
                '\u0930', // cf -- Consonant RA
                '\u0931', // d0 -- Consonant Hard RA ( Southern Scripts )
                '\u0932', // d1 -- Consonant LA
                '\u0933', // d2 -- Consonant Hard LA
                '\u0934', // d3 -- Consonant ZHA ( Tamil & Malayalam )
                '\u0935', // d4 -- Consonant VA
                '\u0936', // d5 -- Consonant SHA
                '\u0937', // d6 -- Consonant Hard SHA
                '\u0938', // d7 -- Consonant SA
                '\u0939', // d8 -- Consonant HA
            };

            checkRange(0xb3, 0xd8, expectConsChars, "INDIC CONSONANTS");

            char[] matraChars = {
                '\u093e', // da -- Vowel Sign AA
                '\u093f', // db -- Vowel Sign I
                '\u0940', // dc -- Vowel Sign II
                '\u0941', // dd -- Vowel Sign U
                '\u0942', // de -- Vowel Sign UU
                '\u0943', // df -- Vowel Sign RI
                '\u0946', // e0 -- Vowel Sign E ( Southern Scripts )
                '\u0947', // e1 -- Vowel Sign EY
                '\u0948', // e2 -- Vowel Sign AI
                '\u0945', // e3 -- Vowel Sign AYE ( Devanagari Script )
                '\u094a', // e4 -- Vowel Sign O ( Southern Scripts )
                '\u094b', // e5 -- Vowel Sign OW
                '\u094c', // e6 -- Vowel Sign AU
                '\u0949' // e7 -- Vowel Sign AWE ( Devanagari Script )
            };

            // Matras or Vowel signs alter the implicit
            // vowel sound associated with an Indic consonant.

            checkRange(0xda, 0xe7, matraChars, "INDIC MATRAS");

            char[] loneContextModifierChars = {
            '\u094d', // e8 -- Vowel Omission Sign ( Halant )
            '\u093c', // e9 -- Diacritic Sign ( Nukta )
            '\u0964' // ea -- Full Stop ( Viram, Northern Scripts )
            };

            checkRange(0xe8, 0xea,
                       loneContextModifierChars, "LONE INDIC CONTEXT CHARS");


            // Test Indic script numeral chars
            // (as opposed to international numerals)

            char[] expectNumeralChars =
            {
                '\u0966', // f1 -- Digit 0
                '\u0967', // f2 -- Digit 1
                '\u0968', // f3 -- Digit 2
                '\u0969', // f4 -- Digit 3
                '\u096a', // f5 -- Digit 4
                '\u096b', // f6 -- Digit 5
                '\u096c', // f7 -- Digit 6
                '\u096d', // f8 -- Digit 7
                '\u096e', // f9 -- Digit 8
                '\u096f'  // fa -- Digit 9
            };

            checkRange(0xf1, 0xfa,
                       expectNumeralChars, "NUMERAL/DIGIT CHARACTERS");
            int lookupOffset = 0;

            char[] expectNuktaSub = {
                '\u0950',
                '\u090c',
                '\u0961',
                '\u0960',
                '\u0962',
                '\u0963',
                '\u0944',
                '\u093d'
            };

            /*
             * ISCII uses a number of code extension techniques
             * to access a number of lesser used characters.
             * The Nukta character which ordinarily signifies
             * a diacritic is used in combination with existing
             * characters to escape them to a different character.
             * value.
            */

            byte[] codeExtensionBytes = {
                (byte)0xa1 , (byte)0xe9, // Chandrabindu + Nukta
                                         // =>DEVANAGARI OM SIGN
                (byte)0xa6 , (byte)0xe9, // Vowel I + Nukta
                                         // => DEVANAGARI VOCALIC L
                (byte)0xa7 , (byte)0xe9, // Vowel II + Nukta
                                         // => DEVANAGARI VOCALIC LL
                (byte)0xaa , (byte)0xe9, // Vowel RI + Nukta
                                         // => DEVANAGARI VOCALIC RR
                (byte)0xdb , (byte)0xe9, //  Vowel sign I + Nukta
                                         // => DEVANAGARI VOWEL SIGN VOCALIC L
                (byte)0xdc , (byte)0xe9, // Vowel sign II + Nukta
                                         // => DEVANAGARI VOWEL SIGN VOCALIC LL

                (byte)0xdf , (byte)0xe9, // Vowel sign Vocalic R + Nukta
                                         // => DEVANAGARI VOWEL SIGN VOCALIC RR
                (byte)0xea , (byte)0xe9  // Full stop/Phrase separator + Nukta
                                         // => DEVANAGARI SIGN AVAGRAHA
            };

            lookupOffset = 0;
            byte[] bytePair = new byte[2];

            for (int i=0; i < (codeExtensionBytes.length)/2; i++ ) {
                bytePair[0] = (byte) codeExtensionBytes[lookupOffset++];
                bytePair[1] = (byte) codeExtensionBytes[lookupOffset++];

                String unicodeStr = new String (bytePair,"ISCII91");
                if (unicodeStr.charAt(0) != expectNuktaSub[i]) {
                    throw new Exception("Failed Nukta Sub");
                }
            }

            lookupOffset = 0;
            byte[] comboBytes = {
                (byte)0xe8 , (byte)0xe8, //HALANT + HALANT
                (byte)0xe8 , (byte)0xe9  //HALANT + NUKTA    aka. Soft Halant
            };
            char[] expectCombChars = {
                '\u094d',
                '\u200c',
                '\u094d',
                '\u200d'
            };

            for (int i=0; i < (comboBytes.length)/2; i++ ) {
                bytePair[0] = (byte) comboBytes[lookupOffset++];
                bytePair[1] = (byte) comboBytes[lookupOffset];
                String unicodeStr = new String (bytePair, "ISCII91");
                if (unicodeStr.charAt(0) != expectCombChars[lookupOffset-1]
                    && unicodeStr.charAt(1) != expectCombChars[lookupOffset]) {
                    throw new Exception("Failed ISCII91 Regression Test");
                }
                lookupOffset++;
            }

        } catch (UnsupportedEncodingException e) {
             System.err.println ("ISCII91 encoding not supported");
             throw new Exception ("Failed ISCII91 Regression Test");
        }
    }

    public static void main (String[] args) throws Exception {
        test();
    }
}
