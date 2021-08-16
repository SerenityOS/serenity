/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */


package sun.nio.cs.ext;

import java.nio.charset.Charset;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.Surrogate;
import sun.nio.cs.HistoricallyNamedCharset;

public class ISCII91 extends Charset implements HistoricallyNamedCharset
{
    private static final char NUKTA_CHAR = '\u093c';
    private static final char HALANT_CHAR = '\u094d';
    private static final byte NO_CHAR = (byte)255;

    public ISCII91() {
        super("x-ISCII91", ExtendedCharsets.aliasesFor("x-ISCII91"));
    }

    public String historicalName() {
        return "ISCII91";
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof ISCII91));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static final char[] directMapTable = {
        '\u0000', // ascii character
        '\u0001', // ascii character
        '\u0002', // ascii character
        '\u0003', // ascii character
        '\u0004', // ascii character
        '\u0005', // ascii character
        '\u0006', // ascii character
        '\u0007', // ascii character
        '\u0008', // ascii character
        '\u0009', // ascii character
        '\012', // ascii character
        '\u000b', // ascii character
        '\u000c', // ascii character
        '\015', // ascii character
        '\u000e', // ascii character
        '\u000f', // ascii character
        '\u0010', // ascii character
        '\u0011', // ascii character
        '\u0012', // ascii character
        '\u0013', // ascii character
        '\u0014', // ascii character
        '\u0015', // ascii character
        '\u0016', // ascii character
        '\u0017', // ascii character
        '\u0018', // ascii character
        '\u0019', // ascii character
        '\u001a', // ascii character
        '\u001b', // ascii character
        '\u001c', // ascii character
        '\u001d', // ascii character
        '\u001e', // ascii character
        '\u001f', // ascii character
        '\u0020', // ascii character
        '\u0021', // ascii character
        '\u0022', // ascii character
        '\u0023', // ascii character
        '\u0024', // ascii character
        '\u0025', // ascii character
        '\u0026', // ascii character
        (char)0x0027, // '\u0027' control -- ascii character
        '\u0028', // ascii character
        '\u0029', // ascii character
        '\u002a', // ascii character
        '\u002b', // ascii character
        '\u002c', // ascii character
        '\u002d', // ascii character
        '\u002e', // ascii character
        '\u002f', // ascii character
        '\u0030', // ascii character
        '\u0031', // ascii character
        '\u0032', // ascii character
        '\u0033', // ascii character
        '\u0034', // ascii character
        '\u0035', // ascii character
        '\u0036', // ascii character
        '\u0037', // ascii character
        '\u0038', // ascii character
        '\u0039', // ascii character
        '\u003a', // ascii character
        '\u003b', // ascii character
        '\u003c', // ascii character
        '\u003d', // ascii character
        '\u003e', // ascii character
        '\u003f', // ascii character
        '\u0040', // ascii character
        '\u0041', // ascii character
        '\u0042', // ascii character
        '\u0043', // ascii character
        '\u0044', // ascii character
        '\u0045', // ascii character
        '\u0046', // ascii character
        '\u0047', // ascii character
        '\u0048', // ascii character
        '\u0049', // ascii character
        '\u004a', // ascii character
        '\u004b', // ascii character
        '\u004c', // ascii character
        '\u004d', // ascii character
        '\u004e', // ascii character
        '\u004f', // ascii character
        '\u0050', // ascii character
        '\u0051', // ascii character
        '\u0052', // ascii character
        '\u0053', // ascii character
        '\u0054', // ascii character
        '\u0055', // ascii character
        '\u0056', // ascii character
        '\u0057', // ascii character
        '\u0058', // ascii character
        '\u0059', // ascii character
        '\u005a', // ascii character
        '\u005b', // ascii character
        '\\',// '\u005c' -- ascii character
        '\u005d', // ascii character
        '\u005e', // ascii character
        '\u005f', // ascii character
        '\u0060', // ascii character
        '\u0061', // ascii character
        '\u0062', // ascii character
        '\u0063', // ascii character
        '\u0064', // ascii character
        '\u0065', // ascii character
        '\u0066', // ascii character
        '\u0067', // ascii character
        '\u0068', // ascii character
        '\u0069', // ascii character
        '\u006a', // ascii character
        '\u006b', // ascii character
        '\u006c', // ascii character
        '\u006d', // ascii character
        '\u006e', // ascii character
        '\u006f', // ascii character
        '\u0070', // ascii character
        '\u0071', // ascii character
        '\u0072', // ascii character
        '\u0073', // ascii character
        '\u0074', // ascii character
        '\u0075', // ascii character
        '\u0076', // ascii character
        '\u0077', // ascii character
        '\u0078', // ascii character
        '\u0079', // ascii character
        '\u007a', // ascii character
        '\u007b', // ascii character
        '\u007c', // ascii character
        '\u007d', // ascii character
        '\u007e', // ascii character
        '\u007f', // ascii character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\uffff', // unknown character
        '\u0901', // a1 -- Vowel-modifier CHANDRABINDU
        '\u0902', // a2 -- Vowel-modifier ANUSWAR
        '\u0903', // a3 -- Vowel-modifier VISARG

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

        '\u200d', // d9 -- Consonant INVISIBLE
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
        '\u0949', // e7 -- Vowel Sign AWE ( Devanagari Script )

        '\u094d', // e8 -- Vowel Omission Sign ( Halant )
        '\u093c', // e9 -- Diacritic Sign ( Nukta )
        '\u0964', // ea -- Full Stop ( Viram, Northern Scripts )

        '\uffff', // eb -- This position shall not be used
        '\uffff', // ec -- This position shall not be used
        '\uffff', // ed -- This position shall not be used
        '\uffff', // ee -- This position shall not be used

        '\ufffd', // ef -- Attribute Code ( ATR )
        '\ufffd', // f0 -- Extension Code ( EXT )

        '\u0966', // f1 -- Digit 0
        '\u0967', // f2 -- Digit 1
        '\u0968', // f3 -- Digit 2
        '\u0969', // f4 -- Digit 3
        '\u096a', // f5 -- Digit 4
        '\u096b', // f6 -- Digit 5
        '\u096c', // f7 -- Digit 6
        '\u096d', // f8 -- Digit 7
        '\u096e', // f9 -- Digit 8
        '\u096f', // fa -- Digit 9

        '\uffff', // fb -- This position shall not be used
        '\uffff', // fc -- This position shall not be used
        '\uffff', // fd -- This position shall not be used
        '\uffff', // fe -- This position shall not be used
        '\uffff'  // ff -- This position shall not be used
    }; //end of table definition

    private static final byte[] encoderMappingTable = {
    NO_CHAR,NO_CHAR, //0900 <reserved>
    (byte)161,NO_CHAR, //0901 -- DEVANAGARI SIGN CANDRABINDU = anunasika
    (byte)162,NO_CHAR, //0902 -- DEVANAGARI SIGN ANUSVARA = bindu
    (byte)163,NO_CHAR, //0903 -- DEVANAGARI SIGN VISARGA
    NO_CHAR,NO_CHAR, //0904 <reserved>
    (byte)164,NO_CHAR, //0905 -- DEVANAGARI LETTER A
    (byte)165,NO_CHAR, //0906 -- DEVANAGARI LETTER AA
    (byte)166,NO_CHAR, //0907 -- DEVANAGARI LETTER I
    (byte)167,NO_CHAR, //0908 -- DEVANAGARI LETTER II
    (byte)168,NO_CHAR, //0909 -- DEVANAGARI LETTER U
    (byte)169,NO_CHAR, //090a -- DEVANAGARI LETTER UU
    (byte)170,NO_CHAR, //090b -- DEVANAGARI LETTER VOCALIC R
    (byte)166,(byte)233, //090c -- DEVANAGARI LETTER VOVALIC L
    (byte)174,NO_CHAR, //090d -- DEVANAGARI LETTER CANDRA E
    (byte)171,NO_CHAR, //090e -- DEVANAGARI LETTER SHORT E
    (byte)172,NO_CHAR, //090f -- DEVANAGARI LETTER E
    (byte)173,NO_CHAR, //0910 -- DEVANAGARI LETTER AI
    (byte)178,NO_CHAR, //0911 -- DEVANAGARI LETTER CANDRA O
    (byte)175,NO_CHAR, //0912 -- DEVANAGARI LETTER SHORT O
    (byte)176,NO_CHAR, //0913 -- DEVANAGARI LETTER O
    (byte)177,NO_CHAR, //0914 -- DEVANAGARI LETTER AU
    (byte)179,NO_CHAR, //0915 -- DEVANAGARI LETTER KA
    (byte)180,NO_CHAR, //0916 -- DEVANAGARI LETTER KHA
    (byte)181,NO_CHAR, //0917 -- DEVANAGARI LETTER GA
    (byte)182,NO_CHAR, //0918 -- DEVANAGARI LETTER GHA
    (byte)183,NO_CHAR, //0919 -- DEVANAGARI LETTER NGA
    (byte)184,NO_CHAR, //091a -- DEVANAGARI LETTER CA
    (byte)185,NO_CHAR, //091b -- DEVANAGARI LETTER CHA
    (byte)186,NO_CHAR, //091c -- DEVANAGARI LETTER JA
    (byte)187,NO_CHAR, //091d -- DEVANAGARI LETTER JHA
    (byte)188,NO_CHAR, //091e -- DEVANAGARI LETTER NYA
    (byte)189,NO_CHAR, //091f -- DEVANAGARI LETTER TTA
    (byte)190,NO_CHAR, //0920 -- DEVANAGARI LETTER TTHA
    (byte)191,NO_CHAR, //0921 -- DEVANAGARI LETTER DDA
    (byte)192,NO_CHAR, //0922 -- DEVANAGARI LETTER DDHA
    (byte)193,NO_CHAR, //0923 -- DEVANAGARI LETTER NNA
    (byte)194,NO_CHAR, //0924 -- DEVANAGARI LETTER TA
    (byte)195,NO_CHAR, //0925 -- DEVANAGARI LETTER THA
    (byte)196,NO_CHAR, //0926 -- DEVANAGARI LETTER DA
    (byte)197,NO_CHAR, //0927 -- DEVANAGARI LETTER DHA
    (byte)198,NO_CHAR, //0928 -- DEVANAGARI LETTER NA
    (byte)199,NO_CHAR, //0929 -- DEVANAGARI LETTER NNNA <=> 0928 + 093C
    (byte)200,NO_CHAR, //092a -- DEVANAGARI LETTER PA
    (byte)201,NO_CHAR, //092b -- DEVANAGARI LETTER PHA
    (byte)202,NO_CHAR, //092c -- DEVANAGARI LETTER BA
    (byte)203,NO_CHAR, //092d -- DEVANAGARI LETTER BHA
    (byte)204,NO_CHAR, //092e -- DEVANAGARI LETTER MA
    (byte)205,NO_CHAR, //092f -- DEVANAGARI LETTER YA
    (byte)207,NO_CHAR, //0930 -- DEVANAGARI LETTER RA
    (byte)208,NO_CHAR, //0931 -- DEVANAGARI LETTER RRA <=> 0930 + 093C
    (byte)209,NO_CHAR, //0932 -- DEVANAGARI LETTER LA
    (byte)210,NO_CHAR, //0933 -- DEVANAGARI LETTER LLA
    (byte)211,NO_CHAR, //0934 -- DEVANAGARI LETTER LLLA <=> 0933 + 093C
    (byte)212,NO_CHAR, //0935 -- DEVANAGARI LETTER VA
    (byte)213,NO_CHAR, //0936 -- DEVANAGARI LETTER SHA
    (byte)214,NO_CHAR, //0937 -- DEVANAGARI LETTER SSA
    (byte)215,NO_CHAR, //0938 -- DEVANAGARI LETTER SA
    (byte)216,NO_CHAR, //0939 -- DEVANAGARI LETTER HA
    NO_CHAR,NO_CHAR, //093a <reserved>
    NO_CHAR,NO_CHAR, //093b <reserved>
    (byte)233,NO_CHAR, //093c -- DEVANAGARI SIGN NUKTA
    (byte)234,(byte)233, //093d -- DEVANAGARI SIGN AVAGRAHA
    (byte)218,NO_CHAR, //093e -- DEVANAGARI VOWEL SIGN AA
    (byte)219,NO_CHAR, //093f -- DEVANAGARI VOWEL SIGN I
    (byte)220,NO_CHAR, //0940 -- DEVANAGARI VOWEL SIGN II
    (byte)221,NO_CHAR, //0941 -- DEVANAGARI VOWEL SIGN U
    (byte)222,NO_CHAR, //0942 -- DEVANAGARI VOWEL SIGN UU
    (byte)223,NO_CHAR, //0943 -- DEVANAGARI VOWEL SIGN VOCALIC R
    (byte)223,(byte)233, //0944 -- DEVANAGARI VOWEL SIGN VOCALIC RR
    (byte)227,NO_CHAR, //0945 -- DEVANAGARI VOWEL SIGN CANDRA E
    (byte)224,NO_CHAR, //0946 -- DEVANAGARI VOWEL SIGN SHORT E
    (byte)225,NO_CHAR, //0947 -- DEVANAGARI VOWEL SIGN E
    (byte)226,NO_CHAR, //0948 -- DEVANAGARI VOWEL SIGN AI
    (byte)231,NO_CHAR, //0949 -- DEVANAGARI VOWEL SIGN CANDRA O
    (byte)228,NO_CHAR, //094a -- DEVANAGARI VOWEL SIGN SHORT O
    (byte)229,NO_CHAR, //094b -- DEVANAGARI VOWEL SIGN O
    (byte)230,NO_CHAR, //094c -- DEVANAGARI VOWEL SIGN AU
    (byte)232,NO_CHAR, //094d -- DEVANAGARI SIGN VIRAMA ( halant )
    NO_CHAR,NO_CHAR, //094e <reserved>
    NO_CHAR,NO_CHAR, //094f <reserved>
    (byte)161,(byte)233, //0950 -- DEVANAGARI OM
    (byte)240,(byte)181, //0951 -- DEVANAGARI STRESS SIGN UDATTA
    (byte)240,(byte)184, //0952 -- DEVANAGARI STRESS SIGN ANUDATTA
    (byte)254,NO_CHAR, //0953 -- DEVANAGARI GRAVE ACCENT || MISSING
    (byte)254,NO_CHAR, //0954 -- DEVANAGARI ACUTE ACCENT || MISSING
    NO_CHAR,NO_CHAR, //0955 <reserved>
    NO_CHAR,NO_CHAR, //0956 <reserved>
    NO_CHAR,NO_CHAR, //0957 <reserved>
    (byte)179,(byte)233, //0958 -- DEVANAGARI LETTER QA <=> 0915 + 093C
    (byte)180,(byte)233, //0959 -- DEVANAGARI LETTER KHHA <=> 0916 + 093C
    (byte)181,(byte)233, //095a -- DEVANAGARI LETTER GHHA <=> 0917 + 093C
    (byte)186,(byte)233, //095b -- DEVANAGARI LETTER ZA <=> 091C + 093C
    (byte)191,(byte)233, //095c -- DEVANAGARI LETTER DDDHA <=> 0921 + 093C
    (byte)192,(byte)233, //095d -- DEVANAGARI LETTER RHA <=> 0922 + 093C
    (byte)201,(byte)233, //095e -- DEVANAGARI LETTER FA <=> 092B + 093C
    (byte)206,NO_CHAR, //095f -- DEVANAGARI LETTER YYA <=> 092F + 093C
    (byte)170,(byte)233, //0960 -- DEVANAGARI LETTER VOCALIC RR
    (byte)167,(byte)233, //0961 -- DEVANAGARI LETTER VOCALIC LL
    (byte)219,(byte)233, //0962 -- DEVANAGARI VOWEL SIGN VOCALIC L
    (byte)220,(byte)233, //0963 -- DEVANAGARI VOWEL SIGN VOCALIC LL
    (byte)234,NO_CHAR, //0964 -- DEVANAGARI DANDA ( phrase separator )
    (byte)234,(byte)234, //0965 -- DEVANAGARI DOUBLE DANDA
    (byte)241,NO_CHAR, //0966 -- DEVANAGARI DIGIT ZERO
    (byte)242,NO_CHAR, //0967 -- DEVANAGARI DIGIT ONE
    (byte)243,NO_CHAR, //0968 -- DEVANAGARI DIGIT TWO
    (byte)244,NO_CHAR, //0969 -- DEVANAGARI DIGIT THREE
    (byte)245,NO_CHAR, //096a -- DEVANAGARI DIGIT FOUR
    (byte)246,NO_CHAR, //096b -- DEVANAGARI DIGIT FIVE
    (byte)247,NO_CHAR, //096c -- DEVANAGARI DIGIT SIX
    (byte)248,NO_CHAR, //096d -- DEVANAGARI DIGIT SEVEN
    (byte)249,NO_CHAR, //096e -- DEVANAGARI DIGIT EIGHT
    (byte)250,NO_CHAR, //096f -- DEVANAGARI DIGIT NINE
    (byte)240,(byte)191,  //0970 -- DEVANAGARI ABBREVIATION SIGN
    NO_CHAR,NO_CHAR, //0971 -- reserved
    NO_CHAR,NO_CHAR, //0972 -- reserved
    NO_CHAR,NO_CHAR, //0973 -- reserved
    NO_CHAR,NO_CHAR, //0974 -- reserved
    NO_CHAR,NO_CHAR, //0975 -- reserved
    NO_CHAR,NO_CHAR, //0976 -- reserved
    NO_CHAR,NO_CHAR, //0977 -- reserved
    NO_CHAR,NO_CHAR, //0978 -- reserved
    NO_CHAR,NO_CHAR, //0979 -- reserved
    NO_CHAR,NO_CHAR, //097a -- reserved
    NO_CHAR,NO_CHAR, //097b -- reserved
    NO_CHAR,NO_CHAR, //097c -- reserved
    NO_CHAR,NO_CHAR, //097d -- reserved
    NO_CHAR,NO_CHAR, //097e -- reserved
    NO_CHAR,NO_CHAR  //097f -- reserved
    }; //end of table definition

    private static class Decoder extends CharsetDecoder {

        private static final char ZWNJ_CHAR = '\u200c';
        private static final char ZWJ_CHAR = '\u200d';
        private static final char INVALID_CHAR = '\uffff';

        private char contextChar = INVALID_CHAR;
        private boolean needFlushing = false;


        private Decoder(Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        protected CoderResult implFlush(CharBuffer out) {
            if(needFlushing) {
                if (out.remaining() < 1) {
                    return CoderResult.OVERFLOW;
                } else {
                    out.put(contextChar);
                }
            }
            contextChar = INVALID_CHAR;
            needFlushing = false;
            return CoderResult.UNDERFLOW;
        }

        /* Rules:
         * 1) ATR,EXT,following character to be replaced with '\ufffd'
         * 2) Halant + Halant => '\u094d' (Virama) + '\u200c'(ZWNJ)
         * 3) Halant + Nukta => '\u094d' (Virama) + '\u200d'(ZWJ)
         */
        private CoderResult decodeArrayLoop(ByteBuffer src,
                                             CharBuffer dst)
        {
            byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                while (sp < sl) {
                    int index = sa[sp];
                    index = ( index < 0 )? ( index + 255 ):index;
                    char currentChar = directMapTable[index];

                    // if the contextChar is either ATR || EXT
                    // set the output to '\ufffd'
                    if(contextChar == '\ufffd') {
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = '\ufffd';
                        contextChar = INVALID_CHAR;
                        needFlushing = false;
                        sp++;
                        continue;
                    }

                    switch(currentChar) {
                    case '\u0901':
                    case '\u0907':
                    case '\u0908':
                    case '\u090b':
                    case '\u093f':
                    case '\u0940':
                    case '\u0943':
                    case '\u0964':
                        if(needFlushing) {
                            if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            da[dp++] = contextChar;
                            contextChar = currentChar;
                            sp++;
                            continue;
                        }
                        contextChar = currentChar;
                        needFlushing = true;
                        sp++;
                        continue;
                    case NUKTA_CHAR:
                        if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                        switch(contextChar) {
                        case '\u0901':
                            da[dp++] = '\u0950';
                            break;
                        case '\u0907':
                            da[dp++] = '\u090c';
                            break;
                        case '\u0908':
                            da[dp++] = '\u0961';
                            break;
                        case '\u090b':
                            da[dp++] = '\u0960';
                            break;
                        case '\u093f':
                            da[dp++] = '\u0962';
                            break;
                        case '\u0940':
                            da[dp++] = '\u0963';
                            break;
                        case '\u0943':
                            da[dp++] = '\u0944';
                            break;
                        case '\u0964':
                            da[dp++] = '\u093d';
                            break;
                        case HALANT_CHAR:
                            if(needFlushing) {
                                da[dp++] = contextChar;
                                contextChar = currentChar;
                                sp++;
                                continue;
                            }
                            da[dp++] = ZWJ_CHAR;
                            break;
                        default:
                            if(needFlushing) {
                                da[dp++] = contextChar;
                                contextChar = currentChar;
                                sp++;
                                continue;
                            }
                            da[dp++] = NUKTA_CHAR;
                        }
                        break;
                    case HALANT_CHAR:
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        if(needFlushing) {
                            da[dp++] = contextChar;
                            contextChar = currentChar;
                            sp++;
                            continue;
                        }
                        if(contextChar == HALANT_CHAR) {
                            da[dp++] = ZWNJ_CHAR;
                            break;
                        }
                        da[dp++] = HALANT_CHAR;
                        break;
                    case INVALID_CHAR:
                        if(needFlushing) {
                            if (dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            da[dp++] = contextChar;
                            contextChar = currentChar;
                            sp++;
                            continue;
                        }
                        return CoderResult.unmappableForLength(1);
                    default:
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        if(needFlushing) {
                            da[dp++] = contextChar;
                            contextChar = currentChar;
                            sp++;
                            continue;
                        }
                        da[dp++] = currentChar;
                        break;
                    }//end switch

                contextChar = currentChar;
                needFlushing = false;
                sp++;
            }
            return CoderResult.UNDERFLOW;
           } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
           }
        }

        private CoderResult decodeBufferLoop(ByteBuffer src,
                                             CharBuffer dst)
        {
            int mark = src.position();

            try {
                while (src.hasRemaining()) {
                    int index = src.get();
                    index = ( index < 0 )? ( index + 255 ):index;
                    char currentChar = directMapTable[index];

                    // if the contextChar is either ATR || EXT
                    // set the output to '\ufffd'
                    if(contextChar == '\ufffd') {
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        dst.put('\ufffd');
                        contextChar = INVALID_CHAR;
                        needFlushing = false;
                        mark++;
                        continue;
                    }

                    switch(currentChar) {
                    case '\u0901':
                    case '\u0907':
                    case '\u0908':
                    case '\u090b':
                    case '\u093f':
                    case '\u0940':
                    case '\u0943':
                    case '\u0964':
                        if(needFlushing) {
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            dst.put(contextChar);
                            contextChar = currentChar;
                            mark++;
                            continue;
                        }
                        contextChar = currentChar;
                        needFlushing = true;
                        mark++;
                        continue;
                    case NUKTA_CHAR:
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        switch(contextChar) {
                        case '\u0901':
                            dst.put('\u0950');
                            break;
                        case '\u0907':
                            dst.put('\u090c');
                            break;
                        case '\u0908':
                            dst.put('\u0961');
                            break;
                        case '\u090b':
                            dst.put('\u0960');
                            break;
                        case '\u093f':
                            dst.put('\u0962');
                            break;
                        case '\u0940':
                            dst.put('\u0963');
                            break;
                        case '\u0943':
                            dst.put('\u0944');
                            break;
                        case '\u0964':
                            dst.put('\u093d');
                            break;
                        case HALANT_CHAR:
                            if(needFlushing) {
                                dst.put(contextChar);
                                contextChar = currentChar;
                                mark++;
                                continue;
                            }
                            dst.put(ZWJ_CHAR);
                            break;
                        default:
                            if(needFlushing) {
                                dst.put(contextChar);
                                contextChar = currentChar;
                                mark++;
                                continue;
                            }
                            dst.put(NUKTA_CHAR);
                        }
                        break;
                    case HALANT_CHAR:
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        if(needFlushing) {
                            dst.put(contextChar);
                            contextChar = currentChar;
                            mark++;
                            continue;
                        }
                        if(contextChar == HALANT_CHAR) {
                            dst.put(ZWNJ_CHAR);
                            break;
                        }
                        dst.put(HALANT_CHAR);
                        break;
                    case INVALID_CHAR:
                        if(needFlushing) {
                            if (dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            dst.put(contextChar);
                            contextChar = currentChar;
                            mark++;
                            continue;
                        }
                        return CoderResult.unmappableForLength(1);
                    default:
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        if(needFlushing) {
                            dst.put(contextChar);
                            contextChar = currentChar;
                            mark++;
                            continue;
                        }
                        dst.put(currentChar);
                        break;
                    }//end switch
                contextChar = currentChar;
                needFlushing = false;
                mark++;
                }
            return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
           }
        }

        protected CoderResult decodeLoop(ByteBuffer src,
                                         CharBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }
    }

    private static class Encoder extends CharsetEncoder {

        private static final byte NO_CHAR = (byte)255;

        //private static CharToByteISCII91 c2b = new CharToByteISCII91();
        //private static final byte[] directMapTable = c2b.getISCIIEncoderMap();

        private final Surrogate.Parser sgp = new Surrogate.Parser();

        private Encoder(Charset cs) {
            super(cs, 2.0f, 2.0f);
        }

        public boolean canEncode(char ch) {
            //check for Devanagari range,ZWJ,ZWNJ and ASCII range.
            return ((ch >= '\u0900' && ch <= '\u097f' &&
                     encoderMappingTable[2*(ch-'\u0900')] != NO_CHAR) ||
                    (ch == '\u200d') ||
                    (ch == '\u200c') ||
                    (ch <= '\u007f'));
        }


        private CoderResult encodeArrayLoop(CharBuffer src,
                                             ByteBuffer dst)
        {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();

            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();

            try {
                char inputChar;
                while (sp < sl) {
                    int index = Integer.MIN_VALUE;
                    inputChar = sa[sp];

                    if (inputChar >= 0x0000 && inputChar <= 0x007f) {
                        if (dl - dp < 1)
                            return CoderResult.OVERFLOW;
                        da[dp++] = (byte) inputChar;
                        sp++;
                        continue;
                    }

                    // if inputChar == ZWJ replace it with halant
                    // if inputChar == ZWNJ replace it with Nukta

                    if (inputChar == 0x200c) {
                        inputChar = HALANT_CHAR;
                    }
                    else if (inputChar == 0x200d) {
                        inputChar = NUKTA_CHAR;
                    }

                    if (inputChar >= 0x0900 && inputChar <= 0x097f) {
                        index = ((int)(inputChar) - 0x0900)*2;
                    }

                    if (Character.isSurrogate(inputChar)) {
                        if (sgp.parse(inputChar, sa, sp, sl) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }

                    if (index == Integer.MIN_VALUE ||
                        encoderMappingTable[index] == NO_CHAR) {
                        return CoderResult.unmappableForLength(1);
                    } else {
                        if(encoderMappingTable[index + 1] == NO_CHAR) {
                            if(dl - dp < 1)
                                return CoderResult.OVERFLOW;
                            da[dp++] = encoderMappingTable[index];
                        } else {
                            if(dl - dp < 2)
                                return CoderResult.OVERFLOW;
                            da[dp++] = encoderMappingTable[index];
                            da[dp++] = encoderMappingTable[index + 1];
                        }
                        sp++;
                    }
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private CoderResult encodeBufferLoop(CharBuffer src,
                                             ByteBuffer dst)
        {
            int mark = src.position();

            try {
                char inputChar;
                while (src.hasRemaining()) {
                    int index = Integer.MIN_VALUE;
                    inputChar = src.get();

                    if (inputChar >= 0x0000 && inputChar <= 0x007f) {
                        if (dst.remaining() < 1)
                            return CoderResult.OVERFLOW;
                        dst.put((byte) inputChar);
                        mark++;
                        continue;
                    }

                    // if inputChar == ZWJ replace it with halant
                    // if inputChar == ZWNJ replace it with Nukta

                    if (inputChar == 0x200c) {
                        inputChar = HALANT_CHAR;
                    }
                    else if (inputChar == 0x200d) {
                        inputChar = NUKTA_CHAR;
                    }

                    if (inputChar >= 0x0900 && inputChar <= 0x097f) {
                        index = ((int)(inputChar) - 0x0900)*2;
                    }

                    if (Character.isSurrogate(inputChar)) {
                        if (sgp.parse(inputChar, src) < 0)
                            return sgp.error();
                        return sgp.unmappableResult();
                    }

                    if (index == Integer.MIN_VALUE ||
                        encoderMappingTable[index] == NO_CHAR) {
                        return CoderResult.unmappableForLength(1);
                    } else {
                        if(encoderMappingTable[index + 1] == NO_CHAR) {
                            if(dst.remaining() < 1)
                                return CoderResult.OVERFLOW;
                            dst.put(encoderMappingTable[index]);
                        } else {
                            if(dst.remaining() < 2)
                                return CoderResult.OVERFLOW;
                            dst.put(encoderMappingTable[index]);
                            dst.put(encoderMappingTable[index + 1]);
                        }
                    }
                    mark++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult encodeLoop(CharBuffer src,
                                         ByteBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }
    }
}
