/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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


import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharacterCodingException;
import sun.nio.cs.HistoricallyNamedCharset;

public class IBM949C_OLD extends Charset implements HistoricallyNamedCharset
{

    public IBM949C_OLD() {
        super("x-IBM949C_OLD", null);
    }

    public String historicalName() {
        return "Cp949C";
    }

    public boolean contains(Charset cs) {
        return ((cs.name().equals("US-ASCII"))
                || (cs instanceof IBM949C_OLD));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    public short[] getEncoderIndex1() {
        return Encoder.index1;

    }
    public String getEncoderIndex2() {
        return Encoder.index2;

    }
    public String getEncoderIndex2a() {
        return Encoder.index2a;

    }
    private static class Decoder extends IBM949_OLD.Decoder {
        protected static final String singleByteToChar;

        static {
          String indexs = "";
          for (char c = '\0'; c < '\u0080'; ++c) indexs += c;
              singleByteToChar = indexs +
                                 IBM949_OLD.Decoder.singleByteToChar.substring(indexs.length());
        }

        public Decoder(Charset cs) {
            super(cs, singleByteToChar);
        }
    }

   private static class Encoder extends IBM949_OLD.Encoder {

        protected static String index2a =
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uA8A1\u0000\uD2A6" + // 15000 - 15009
                "\u0000\u0000\uE7F4\uD1D6\u0000\u0000\uE6C2\uB5C7\u0000\u0000" + // 15010 - 15019
                "\u0000\uB5C8\u0000\u0000\u0000\uFCD2\u0000\uEBC8\u0000\u9AFD" + // 15020 - 15029
                "\u0000\uE6C1\u0000\u0000\uECD8\u0000\u0000\u0000\uEDAC\uB5C6" + // 15030 - 15039
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uC8B1\u9EE8\u0000" + // 15040 - 15049
                "\u0000\u0000\u0000\u0000\u0000\u0000\uA0E0\uB5C4\u0000\u0000" + // 15050 - 15059
                "\u0000\u0000\u0000\u0000\u0000\uC5F2\uB5C2\u0000\u0000\u0000" + // 15060 - 15069
                "\uB5C3\u0000\u0000\u0000\uDBB5\u0000\uF3E7\uD8FE\u0000\u0000" + // 15070 - 15079
                "\uE9A9\u0000\uD3C7\u0000\u0000\uDCDD\uF8AE\uB5BB\u0000\u0000" + // 15080 - 15089
                "\u9EE7\uB5BC\uB5BD\u0000\uB5BE\uB5B7\u0000\u0000\uB5B8\uB5B9" + // 15090 - 15099
                "\u0000\uB5BA\u0000\uECA9\u0000\uF2EB\u0000\uFDEF\u0000\uF9F3" + // 15100 - 15109
                "\u0000\uA2A3\u0000\uA1D3\uA2A4\u0000\u0000\u0000\uA1D4\uB5B3" + // 15110 - 15119
                "\u0000\u0000\u0000\uB5B4\u0000\u0000\u0000\uD2FC\u0000\u0000" + // 15120 - 15129
                "\u0000\u0000\u0000\uB4BA\uB4BB\u0000\u0000\uD7EB\u0000\u0000" + // 15130 - 15139
                "\u0000\u0000\u0000\u0000\uF7F7\uDCAC\u0000\u0000\uF5E9\u0000" + // 15140 - 15149
                "\u9DFA\u9EA9\u0000\u0000\u0000\uA8AB\uA9AB\u0000\u0000\u0000" + // 15150 - 15159
                "\u0000\uBFB0\uBFB1\uBFB2\uBFB3\uB5B1\uB5B2\u0000\u0000\u0000" + // 15160 - 15169
                "\u0000\u0000\u0000\uD1A5\uDCB8\u0000\u0000\uCFD9\u0000\u0000" + // 15170 - 15179
                "\uDCCD\uEDFB\u0000\uDEF0\uB5AF\u0000\u0000\u0000\uB5B0\u0000" + // 15180 - 15189
                "\u0000\u0000\uCDA2\uE8AE\u0000\u0000\u0000\uE1BD\uB5A9\uB5AA" + // 15190 - 15199
                "\u0000\uB5AB\uB5AC\uB5AD\u0000\u0000\uEEAE\uD6AE\u0000\u0000" + // 15200 - 15209
                "\u9DA1\u0000\u0000\uDFD5\u0000\u0000\uEDD7\u0000\u0000\u0000" + // 15210 - 15219
                "\u9AAC\u9AB0\u9AB8\u9AC9\u9AD3\uB5A8\u0000\u0000\u0000\u0000" + // 15220 - 15229
                "\u0000\u0000\u0000\uA0D9\uB5A5\uB5A6\u0000\u0000\uB5A7\u0000" + // 15230 - 15239
                "\u0000\u0000\uECEE\u0000\u0000\uDDAA\u0000\u0000\uEABE\uD9B1" + // 15240 - 15249
                "\u0000\u0000\u0000\u0000\u0000\uB8A3\uB8A4\u0000\u0000\uD4F5" + // 15250 - 15259
                "\u0000\uD0C9\uEFA7\uE2EC\u0000\uDBEA\u9EE4\uB5A2\u9EE5\uB5A3" + // 15260 - 15269
                "\u0000\u0000\uB5A4\u0000\uEEBB\uCDB4\u9BF2\uE0F3\uEACD\u0000" + // 15270 - 15279
                "\u0000\u0000\uDCEE\u0000\u0000\uF5EA\uE6E0\uB4F8\u0000\u0000" + // 15280 - 15289
                "\uB4F9\uB4FA\u0000\uB4FB\uB4FC\u9EE3\u0000\u0000\u0000\u0000" + // 15290 - 15299
                "\u0000\u0000\u0000\uA0BE\uB4EF\uB4F0\u0000\uB4F1\uB4F2\uB4F3" + // 15300 - 15309
                "\u0000\u0000\uFBAC\uCFC3\uEBFD\u0000\u0000\u0000\u0000\uCCF6" + // 15310 - 15319
                "\u0000\u0000\uD3BA\u0000\uDBAA\u0000\u0000\u0000\uF7E0\u0000" + // 15320 - 15329
                "\u0000\u0000\uDADA\u0000\uF2DC\uFBD6\uE9B2\uB4EE\u0000\u0000" + // 15330 - 15339
                "\u0000\u0000\u0000\u0000\u0000\u9FC7\uB4EB\uB4EC\u0000\u0000" + // 15340 - 15349
                "\uB4ED\u0000\u0000\u0000\uEAB8\uD1F9\u0000\u0000\u0000\u0000" + // 15350 - 15359
                "\uC6A1\u0000\u0000\u0000\uF9DB\u0000\u0000\u0000\u0000\uF4E6" + // 15360 - 15369
                "\u0000\u0000\uE6C5\uEFD5\uB4E6\uB4E7\uB4E8\uB4E9\u0000\u0000" + // 15370 - 15379
                "\u0000\uB4EA\uB4DC\u0000\u0000\uB4DD\uB4DE\uB4DF\uB4E0\uB4E1" + // 15380 - 15389
                "\u9EE1\u0000\uB4D8\u0000\uB4D9\uB4DA\uB4DB\u0000\uCACC\u0000" + // 15390 - 15399
                "\u0000\u0000\u0000\uFBBF\u0000\u0000\uE3BD\u0000\uCFE1\uF0C0" + // 15400 - 15409
                "\uECDA\u0000\uDDD7\uB4D4\uB4D5\u0000\uB4D6\u0000\uB4D7\u0000" + // 15410 - 15419
                "\u0000\uE4B7\u0000\uEADB\u0000\uF5FA\u9CE8\u0000\uEEF5\u0000" + // 15420 - 15429
                "\uDECE\u0000\u0000\u0000\u0000\uE7F3\uB4D2\u9EE0\uB4D3\u0000" + // 15430 - 15439
                "\u0000\u0000\u0000\u0000\uBDB0\uBDB1\u0000\uBDB2\uB4CF\uB4D0" + // 15440 - 15449
                "\u0000\u0000\uB4D1\u0000\u0000\u0000\uF1BE\u0000\u0000\uD3AC" + // 15450 - 15459
                "\u0000\u0000\uCDCC\u0000\u0000\u0000\u0000\uEDD9\u0000\uFCB1" + // 15460 - 15469
                "\uCCF8\u0000\u0000\uDDC6\uFAD1\u0000\uF7DF\uB4CD\u0000\u0000" + // 15470 - 15479
                "\u0000\uB4CE\u0000\u0000\u0000\uE0C2\u0000\uCAE4\u0000\uE7B7" + // 15480 - 15489
                "\u0000\uD2AF\uDCE5\u0000\u0000\u0000\u0000\uD0A5\uF1B4\uB4C6" + // 15490 - 15499
                "\uB4C7\u0000\uB4C8\u0000\uB4C9\uB4CA\u9EDE\uB4C3\uB4C4\uB4C5" + // 15500 - 15509
                "\u0000\u0000\u0000\u0000\u0000\uBDAC\uBDAD\u0000\u0000\u9AF5" + // 15510 - 15519
                "\uF1E3\uD5EE\u0000\u0000\u0000\u0000\uE3E2\uFBBC\uD9A4\u0000" + // 15520 - 15529
                "\u0000\uDFE9\u0000\uEEDE\u0000\u0000\uF7C2\u0000\uD7A4\uCEC5" + // 15530 - 15539
                "\u0000\u0000\u0000\u0000\uCED5\uD6E6\uB4C0\uB4C1\u0000\u0000" + // 15540 - 15549
                "\uB4C2\u0000\u0000\u0000\uF1BD\u0000\u0000\uE2E7\uFDD7\u0000" + // 15550 - 15559
                "\uD9F8\uD4C2\u0000\u0000\u0000\u0000\uF6E5\u0000\uA2D9\uA2D7" + // 15560 - 15569
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCBCA\u0000\u0000\uD6B7" + // 15570 - 15579
                "\uCDB3\u0000\u0000\u0000\u0000\u9CE9\uB4B8\uB4B9\u0000\u0000" + // 15580 - 15589
                "\u0000\u0000\u0000\u0000\uE7A5\u0000\uD5F5\uD3BE\uB4B7\u0000" + // 15590 - 15599
                "\u0000\u0000\u0000\u0000\u0000\u0000\u9EFE\uB4B5\u0000\u0000" + // 15600 - 15609
                "\u0000\uB4B6\u0000\u0000\u0000\uCCBD\u0000\u0000\uD1A9\uDDCC" + // 15610 - 15619
                "\u0000\uD3D2\u0000\uF5C0\u0000\u0000\u0000\uDFDD\u0000\uA1E7" + // 15620 - 15629
                "\uA1E8\uA1E6\uA1E9\uA1EA\uA2D5\uA2D8\uA2D6\uB4B2\u0000\u0000" + // 15630 - 15639
                "\u0000\u0000\u0000\u0000\u0000\uB7AD\uB4AB\u0000\u0000\uB4AC" + // 15640 - 15649
                "\uB4AD\u0000\u0000\u0000\uE7FD\u0000\u0000\uE6A3\uFBF1\uCBB0" + // 15650 - 15659
                "\uB4A5\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u9EEC\uB4A2" + // 15660 - 15669
                "\uB4A3\u0000\u0000\uB4A4\u0000\u0000\u0000\uD4B8\uEBBE\uDDEF" + // 15670 - 15679
                "\u0000\uDDF0\uDDF1\uB3FB\u0000\u0000\u0000\uB3FC\u0000\u0000" + // 15680 - 15689
                "\u0000\uDFAF\u0000\uCAC3\u0000\u0000\uEEFC\u9EDC\u0000\u0000" + // 15690 - 15699
                "\u0000\u0000\u0000\u0000\u0000\u9EE9\uB3F9\u0000\u0000\u0000" + // 15700 - 15709
                "\u0000\u0000\u0000\u0000\u9EDB\uB3F7\u0000\u0000\u0000\uB3F8" + // 15710 - 15719
                "\u0000\u0000\u0000\uE0E8\u0000\u0000\uD3AB\u0000\uEBDC\uB3F0" + // 15720 - 15729
                "\uB3F1\u0000\uB3F2\u0000\uB3F3\u0000\u0000\u9DEB\u0000\u0000" + // 15730 - 15739
                "\u0000\u0000\u0000\uEADA\uB3EE\u0000\uB3EF\u0000\u0000\u0000" + // 15740 - 15749
                "\u0000\u0000\uBCF2\uBCF3\u0000\uBCF4\uB3EB\uB3EC\u9EDA\u0000" + // 15750 - 15759
                "\uB3ED\u0000\u0000\u0000\uE8E0\u0000\u0000\u0000\u0000\u0000" + // 15760 - 15769
                "\uB3FA\u0000\u0000\u0000\uE7A2\uE4D9\u0000\u0000\u0000\uF0E6" + // 15770 - 15779
                "\u0000\u0000\u0000\uE4B9\uB3EA\u0000\u0000\u0000\u0000\u0000" + // 15780 - 15789
                "\u0000\u0000\u9ED9\uB3E8\u9ED8\u0000\u0000\uB3E9\u0000\u0000" + // 15790 - 15799
                "\u0000\uE9B0\u0000\u0000\u0000\u0000\u0000\uB3DB\uB3DC\u0000" + // 15800 - 15809
                "\uB3DD\uB3E4\uB3E5\u0000\u9ED7\uB3E6\uB3E7\u0000\u0000\u9CE3" + // 15810 - 15819
                "\u0000\u0000\u0000\u0000\uE4B6\u0000\uE5E8\uDCC3\u0000\u0000" + // 15820 - 15829
                "\uEDDE\uD3F2\u0000\u0000\uDCA7\u0000\u0000\uD6E7\u0000\u0000" + // 15830 - 15839
                "\u0000\uFBD9\uEDF7\u0000\u0000\uE5B5\uB3E3\u0000\u0000\u0000" + // 15840 - 15849
                "\u0000\u0000\u0000\u0000\u9ECD\uB3E0\uB3E1\u0000\u0000\uB3E2" + // 15850 - 15859
                "\u0000\u0000\u9ED6\uB3DE\uB3DF\u0000\u0000\u0000\u0000\u0000" + // 15860 - 15869
                "\u0000\uDEB0\u0000\u0000\u0000\uD5B2\u0000\u0000\u0000\uD5BC" + // 15870 - 15879
                "\u0000\uCBA8\uEBBC\uE4BE\u0000\u0000\u0000\u0000\u0000\uCFCF" + // 15880 - 15889
                "\u0000\u0000\u0000\uEDB9\uF1C5\u0000\uF3CF\uD7AB\uB3D9\u0000" + // 15890 - 15899
                "\u0000\u0000\uB3DA\u0000\u0000\u0000\uCFED\u0000\uEDEB\u0000" + // 15900 - 15909
                "\u0000\u0000\uF0EC\u0000\u0000\u0000\u0000\uDCB3\u0000\u0000" + // 15910 - 15919
                "\u0000\u0000\uC2EA\u0000\u0000\u0000\uEFB2\u0000\u0000\u0000" + // 15920 - 15929
                "\u0000\uF1DA\u0000\uFAF2\u0000\u0000\uE8C3\u0000\uF1C8\u0000" + // 15930 - 15939
                "\u0000\u0000\uCEF1\uB3D1\uB3D2\u0000\uB3D3\uB3D4\uB3D5\u9ED5" + // 15940 - 15949
                "\u0000\uCFDC\u0000\uD3D1\u0000\u0000\uCCB1\uF7D8\u0000\uA5A9" + // 15950 - 15959
                "\uA5AA\u0000\u0000\u0000\u0000\u0000\u0000\uDFA8\u0000\u0000" + // 15960 - 15969
                "\uF5B6\u0000\u0000\u0000\u0000\u0000\u0000\uF4E9\uD6EC\uEBD3" + // 15970 - 15979
                "\uB3CE\u0000\uB3CF\uB3D0\u0000\u0000\u0000\u0000\uD8D0\u0000" + // 15980 - 15989
                "\uF0C8\uD1A1\uD1A2\uB3CA\uB3CB\u0000\uB3CC\uB3CD\u0000\u0000" + // 15990 - 15999
                "\u9ED4\uB3C8\u0000\u0000\u0000\u0000\uB3C9\u0000\u0000\u9AFC" + // 16000 - 16009
                "\u0000\uD3B1\u0000\u0000\u0000\u0000\uDAE4\u0000\u0000\u0000" + // 16010 - 16019
                "\u9CB1\uB3C7\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u9EBD" + // 16020 - 16029
                "\uB3C4\uB3C5\u0000\u0000\uB3C6\u0000\u0000\u0000\u9BC7\u0000" + // 16030 - 16039
                "\uD5B1\u0000\u0000\u0000\uFBAF\u0000\u0000\u0000\uCBD1\uB3C2" + // 16040 - 16049
                "\uB3C3\u0000\u0000\u0000\u0000\u0000\u0000\uCCD7\uE5C2\u0000" + // 16050 - 16059
                "\u0000\uF6C9\u9AE9\u0000\u0000\u0000\u0000\u0000\u9EFD\uB7E4" + // 16060 - 16069
                "\u0000\uB7E5\uB3BD\u0000\u0000\u9ED2\uB3BE\u0000\u0000\u0000" + // 16070 - 16079
                "\uD5EA\uF1EE\u0000\u0000\u0000\u9AF7\uB3B2\uB3B3\u0000\uB3B4" + // 16080 - 16089
                "\uB3B5\uB3B6\uB3B7\uB3B8\uB3AF\uB3B0\uB3B1\u0000\u0000\u0000" + // 16090 - 16099
                "\u0000\u0000\uBCEE\uBCEF\u0000\u0000\u9CB8\u0000\u0000\u0000" + // 16100 - 16109
                "\u0000\u0000\u0000\uDAE1\u0000\uD6B6\u0000\uF3F1\u0000\u0000" + // 16110 - 16119
                "\u0000\uE3D0\u0000\u0000\uF2FB\uB3AA\uB3AB\uB3AC\u0000\uB3AD" + // 16120 - 16129
                "\u0000\u0000\uB3AE\u9ED1\uB3A9\u0000\u0000\u0000\u0000\u0000" + // 16130 - 16139
                "\u0000\uEBF4\u0000\u0000\u9BED\uB3A4\u0000\u0000\u0000\uB3A5" + // 16140 - 16149
                "\u0000\u0000\u0000\uD4A2\uCFF6\u0000\u0000\u0000\u0000\uC5B4" + // 16150 - 16159
                "\uC5B5\u0000\uC5B6\u9ED0\u0000\u0000\u0000\u0000\u0000\u0000" + // 16160 - 16169
                "\u0000\uB1CC\uB2F6\u0000\uB2F7\u0000\uB2F8\u0000\uB2F9\u0000" + // 16170 - 16179
                "\uDCFC\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE1B0\uB2F3" + // 16180 - 16189
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE4CB\uB2EF\u0000" + // 16190 - 16199
                "\u0000\u0000\uB2F0\u0000\u0000\u0000\uDBE0\u0000\u0000\u0000" + // 16200 - 16209
                "\u0000\u0000\u9ED3\u0000\u0000\u0000\uE6E7\u0000\u0000\uEAC7" + // 16210 - 16219
                "\u0000\uF1D8\u0000\u0000\uD8D8\u0000\u0000\uE0F2\u0000\uA5A1" + // 16220 - 16229
                "\uA5A2\uA5A3\uA5A4\uA5A5\uA5A6\uA5A7\uA5A8\uB2EB\uB2EC\u0000" + // 16230 - 16239
                "\u0000\uB2ED\u0000\u0000\u0000\uE4C5\u0000\u0000\u9DB9\u0000" + // 16240 - 16249
                "\u0000\uCDCB\u0000\u0000\u0000\u0000\u0000\u0000\uEDA8\uDEC2" + // 16250 - 16259
                "\uF6E2\uEDDC\uB2EA\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 16260 - 16269
                "\uEED2\uB2E7\uB2E8\u0000\u0000\uB2E9\u0000\u0000\u0000\u9BF7" + // 16270 - 16279
                "\uCFB0\uF7D9\uF3E6\u9BF5\u0000\uEBD9\u0000\uCFA7\uEAAF\u0000" + // 16280 - 16289
                "\u0000\u0000\u0000\uC2BC\uC2BD\u0000\u0000\uF2ED\u0000\uDBD9" + // 16290 - 16299
                "\u0000\uF0A8\u0000\u0000\uDBDF\uD3D3\uF8C7\u0000\u0000\u0000" + // 16300 - 16309
                "\u0000\uC2EE\uC2EF\u0000\u0000\u9AEE\uCACE\uF8C1\uD2B4\u0000" + // 16310 - 16319
                "\u0000\uDCB4\uB2E5\uB2E6\u0000\u0000\u0000\u0000\u0000\u0000" + // 16320 - 16329
                "\uDAE6\uF7B3\u0000\u0000\uCBB9\u0000\u0000\uEDF9\u0000\u0000" + // 16330 - 16339
                "\u0000\uD1D3\u0000\uE5F0\u0000\u0000\u0000\uD8ED\uE3C4\uF0F1" + // 16340 - 16349
                "\u0000\u0000\uD4CD\u0000\u9DAB\uF3B8\u0000\u0000\u0000\uA0CF" + // 16350 - 16359
                "\uC3E8\u0000\u0000\u0000\uC8B6\u0000\uC8B7\u0000\u0000\uDAC8" + // 16360 - 16369
                "\uDFA6\u0000\uF9B3\uF2D2\u0000\uCAC4\u9ECC\u0000\u0000\u0000" + // 16370 - 16379
                "\uB2E4\u0000\u0000\u0000\uE5C5\u0000\u0000\u0000\u0000\u0000" + // 16380 - 16389
                "\uB3A6\uB3A7\u0000\uB3A8\uB2DE\uB2DF\u0000\uB2E0\u0000\uB2E1" + // 16390 - 16399
                "\uB2E2\u0000\uD3DC\u0000\u0000\uFAFE\u9AE7\u0000\u0000\u0000" + // 16400 - 16409
                "\uDDFB\u0000\u0000\u0000\u0000\uA7A4\u0000\u0000\uA2E0\u0000" + // 16410 - 16419
                "\uA5B8\uA5B9\u0000\u0000\u0000\u0000\u0000\u0000\uF9C2\u0000" + // 16420 - 16429
                "\uEABC\uB2DC\u0000\u0000\u0000\u0000\u0000\u0000\uB2DD\uB2D9" + // 16430 - 16439
                "\uB2DA\u0000\u0000\uB2DB\u0000\u0000\u9ECB\uB2D5\uB2D6\u0000" + // 16440 - 16449
                "\u9EC9\u0000\uB2D7\u0000\u0000\u9BE1\u0000\uE2F2\u9CEB\u0000" + // 16450 - 16459
                "\u0000\u0000\uEEB9\u0000\u0000\u0000\u0000\uD5E3\uB2D4\u0000" + // 16460 - 16469
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE8A8\uB2D2\u0000\u0000" + // 16470 - 16479
                "\u0000\uB2D3\u0000\u0000\u0000\u9BF4\u0000\u0000\u0000\u0000" + // 16480 - 16489
                "\u0000\uB3A2\uB3A3\u0000\u0000\uF5AF\u0000\u9CDC\u0000\u0000" + // 16490 - 16499
                "\uCEF0\u0000\uCCD0\u0000\u0000\u0000\u0000\uCFA6\u0000\u0000" + // 16500 - 16509
                "\uF7B6\u0000\u0000\u0000\u0000\uF4DE\u0000\uA5B0\uA5B1\uA5B2" + // 16510 - 16519
                "\uA5B3\uA5B4\uA5B5\uA5B6\uA5B7\u9EC8\u0000\u0000\u0000\u0000" + // 16520 - 16529
                "\u0000\u0000\u0000\uCFCE\u9EC6\u0000\u0000\u9EC7\uB2CD\uB2CE" + // 16530 - 16539
                "\u0000\u0000\uF3F9\u0000\uEDF8\u0000\uF5C7\u0000\u0000\uF6C4" + // 16540 - 16549
                "\u0000\u0000\u0000\uEEDD\uE7C4\u0000\uF1A6\uCBD5\u0000\u0000" + // 16550 - 16559
                "\u0000\u0000\u0000\u0000\uE1A3\uD2E0\u0000\uA2B6\u0000\uA1C7" + // 16560 - 16569
                "\uA1C8\u0000\u0000\u0000\u0000\uBFC0\uBFC1\u0000\u0000\uCAA9" + // 16570 - 16579
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF7B0\u0000\uCCEA\uB2CC" + // 16580 - 16589
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u9BD5\uB2CA\uB2CB" + // 16590 - 16599
                "\u0000\u0000\u9EC5\u0000\u0000\u0000\uDFF8\u0000\u0000\u0000" + // 16600 - 16609
                "\u0000\u0000\uB2EE\u9ECE\u0000\u0000\uD9A1\u0000\uD8C0\uDCDB" + // 16610 - 16619
                "\u0000\u0000\uEDBD\uB2C1\u0000\uB2C2\u9EC4\uB2C3\u0000\u0000" + // 16620 - 16629
                "\u0000\uDCEA\u0000\u0000\uF0F7\u0000\uF0CA\uB2BE\u0000\u0000" + // 16630 - 16639
                "\u0000\u0000\u0000\u0000\u0000\uD7F7\uB2BC\u0000\u0000\u0000" + // 16640 - 16649
                "\u0000\uB2BD\u0000\u0000\uDEEE\u0000\u0000\u0000\u0000\u9DF2" + // 16650 - 16659
                "\u0000\uF2A3\u0000\uF7F8\u0000\u0000\u0000\u0000\uD0B3\uB2B9" + // 16660 - 16669
                "\u0000\u0000\u0000\uB2BA\u0000\u0000\u0000\uF1BB\u0000\u0000" + // 16670 - 16679
                "\u0000\u9CF2\uE9F1\uB2B5\u0000\u0000\uB2B6\u0000\uB2B7\u0000" + // 16680 - 16689
                "\u0000\uE9C8\u0000\uCBCF\u0000\uE3C9\u0000\u0000\uF6E0\u0000" + // 16690 - 16699
                "\u0000\u0000\u0000\uE9F3\uF2C3\uB2B2\uB2B3\u0000\u0000\uB2B4" + // 16700 - 16709
                "\u0000\u0000\u0000\uE2E3\uEEFB\u0000\u0000\uDFF7\uD7CA\uB2B0" + // 16710 - 16719
                "\uB2B1\u0000\u0000\u0000\u0000\u0000\u0000\uE1B8\u0000\uE8F4" + // 16720 - 16729
                "\uD3FD\uB2AB\u0000\u0000\u0000\uB2AC\u0000\u0000\u0000\u9DA3" + // 16730 - 16739
                "\u0000\u0000\u0000\u0000\u0000\uB2D8\u0000\u0000\u0000\uF1CD" + // 16740 - 16749
                "\u0000\u0000\u0000\u0000\uD2B3\uD2BF\u0000\u0000\u0000\uE3F4" + // 16750 - 16759
                "\uCDD0\u0000\u0000\u9BCE\u9EC2\u0000\u0000\u0000\u0000\u0000" + // 16760 - 16769
                "\u0000\u0000\uCBBE\uB1FE\uB2A1\u0000\uB2A2\uB2A3\uB2A4\u0000" + // 16770 - 16779
                "\u0000\uDBBC\u0000\u0000\u0000\u0000\u0000\u0000\uA2D0\u0000" + // 16780 - 16789
                "\uA2D1\u0000\uF2A2\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 16790 - 16799
                "\uFDBD\uB1FD\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE4D5" + // 16800 - 16809
                "\uB1FA\uB1FB\u0000\u0000\uB1FC\u0000\u0000\u0000\uEAF6\u0000" + // 16810 - 16819
                "\u0000\uF6F9\u9CFA\u0000\uEEA4\u0000\u0000\u0000\u0000\uD0A4" + // 16820 - 16829
                "\u0000\u0000\u9ACF\u0000\u0000\u0000\uE1DE\uCBEE\u0000\uA2D3" + // 16830 - 16839
                "\uA2D4\u0000\u0000\u0000\uA1A5\uA1A6\u0000\uA3A1\uA3A2\uA3A3" + // 16840 - 16849
                "\uA3A4\uA3A5\uA3A6\uA3A7\uB1F7\uB1F8\u9EC1\u0000\u0000\uB1F9" + // 16850 - 16859
                "\u0000\u0000\uD9D5\u0000\u0000\uDFAA\u0000\u0000\u0000\uEAE3" + // 16860 - 16869
                "\u0000\u0000\u0000\u0000\u0000\u9CD8\u0000\u0000\u0000\uDCB9" + // 16870 - 16879
                "\u0000\u0000\u0000\uF1C0\uB1F1\u0000\u0000\u0000\uB1F2\u0000" + // 16880 - 16889
                "\uB1F3\u0000\uD3A5\u0000\u0000\u0000\u9EA2\u0000\u0000\uF7CF" + // 16890 - 16899
                "\uB1E8\uB1E9\u0000\uB1EA\u9EBF\uB1EB\uB1EC\u0000\uCFEA\u0000" + // 16900 - 16909
                "\u0000\uCFD0\u0000\uEACC\u0000\u0000\uD0F9\uECAB\uDED3\uF7E9" + // 16910 - 16919
                "\u9DE3\u0000\uF9F5\uB1E6\u0000\uB1E7\u0000\u9EBE\u0000\u0000" + // 16920 - 16929
                "\u0000\uD8DD\u0000\uCDFD\uF2AB\u0000\u0000\u9DD6\u0000\u0000" + // 16930 - 16939
                "\u0000\uD0AD\u0000\u0000\uF2C2\uF6C3\u0000\uD7D2\u0000\u0000" + // 16940 - 16949
                "\uF9A2\uB1E2\uB1E3\u0000\u0000\uB1E4\u0000\u0000\uB1E5\uB1DD" + // 16950 - 16959
                "\uB1DE\u0000\uB1DF\u0000\uB1E0\u0000\u9EBB\uB1DB\uB1DC\u9EBA" + // 16960 - 16969
                "\u0000\u0000\u0000\u0000\u0000\uBCE2\u0000\u0000\u0000\uEDE7" + // 16970 - 16979
                "\uFBB5\uF8EC\u0000\u0000\u0000\uCEF2\u0000\uD6D9\u0000\u0000" + // 16980 - 16989
                "\u9BD9\u0000\u0000\u0000\u0000\u0000\uEECA\uB1D7\uB1D8\u0000" + // 16990 - 16999
                "\u0000\uB1D9\u0000\u0000\uB1DA\uB1D5\u0000\u0000\u0000\uB1D6" + // 17000 - 17009
                "\u0000\u0000\u0000\u9DAE\u0000\uE7FB\uFCB7\uFCE4\uFBC5\uB1D1" + // 17010 - 17019
                "\uB1D2\u0000\uB1D3\u0000\u9EB8\u0000\u0000\uE7A8\u0000\u0000" + // 17020 - 17029
                "\u0000\u0000\u0000\u0000\uA1E5\uA1E4\u0000\u0000\uF2F1\u0000" + // 17030 - 17039
                "\u0000\u0000\u0000\u0000\u0000\uB8D2\u0000\u0000\uE0FA\uEEC4" + // 17040 - 17049
                "\uD9DE\u0000\u0000\u0000\u0000\uC6DB\uC6DC\u0000\u0000\uFCC1" + // 17050 - 17059
                "\u0000\uEEAB\uD4A5\u9AEA\u0000\u0000\uFDC3\u0000\u0000\u0000" + // 17060 - 17069
                "\uEBF6\uCFB2\u0000\uCDDD\u0000\u0000\u0000\u0000\u0000\u0000" + // 17070 - 17079
                "\u0000\uD9FD\uB1D0\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 17080 - 17089
                "\uD6DB\uB1CD\uB1CE\u0000\u0000\uB1CF\u0000\u0000\u0000\uDCB6" + // 17090 - 17099
                "\uE4E9\u0000\u0000\u0000\u0000\uC4BF\uC4C0\u0000\u0000\uFDC0" + // 17100 - 17109
                "\u0000\u0000\u0000\u0000\u0000\u0000\uA6A3\uA6C8\uA6C7\uA6AE" + // 17110 - 17119
                "\uB1C8\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u9BAA\uB1C5" + // 17120 - 17129
                "\uB1C6\u0000\u0000\uB1C7\u0000\u0000\u0000\uE8DA\uDAC3\uDAC4" + // 17130 - 17139
                "\uD4C5\u0000\uE7FA\uB1BA\u0000\u0000\uB1BB\uB1BC\uB1BD\uB1BE" + // 17140 - 17149
                "\u0000\uECD7\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uEAE6" + // 17150 - 17159
                "\uB1B5\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF1E7\uB1B3" + // 17160 - 17169
                "\u0000\u0000\u0000\uB1B4\u0000\u0000\u0000\uD7B2\u0000\u0000" + // 17170 - 17179
                "\u0000\u0000\uD0FD\uB1AD\u0000\u0000\u0000\uB1AE\u0000\u0000" + // 17180 - 17189
                "\u0000\uD6B0\uF8CA\u0000\uFCFA\u0000\uD9FE\u9EB5\uB1A8\u0000" + // 17190 - 17199
                "\u9EB6\uB1A9\uB1AA\u0000\u0000\uF5D7\u0000\u0000\uD8BF\u0000" + // 17200 - 17209
                "\u0000\u0000\u9BA9\u0000\u0000\uD0C1\u0000\u0000\uDCBC\uD2B6" + // 17210 - 17219
                "\uF5D5\u0000\u0000\u0000\u0000\uC8C4\uC8C5\u0000\u0000\uCEEA" + // 17220 - 17229
                "\u0000\u0000\u0000\u0000\u0000\u0000\u9BBA\u9AF2\uEAE0\uB1A7" + // 17230 - 17239
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD9F6\uB1A5\u9EB4" + // 17240 - 17249
                "\u0000\u0000\uB1A6\u0000\u0000\u0000\uCDBB\u0000\uEFDA\uEED8" + // 17250 - 17259
                "\u0000\uDDA7\uB0FC\u0000\u0000\u0000\uB0FD\u0000\uB0FE\u0000" + // 17260 - 17269
                "\u9DA7\u0000\u9EA7\u0000\u0000\u0000\u0000\u0000\uCFD5\uD8FD" + // 17270 - 17279
                "\u0000\u0000\uE2DE\uE1B5\u0000\u0000\uCDEF\uF1A7\uCEE5\uB0F5" + // 17280 - 17289
                "\uB0F6\u0000\uB0F7\u0000\uB0F8\uB0F9\u0000\uD0EF\u0000\u9DB2" + // 17290 - 17299
                "\uFDED\u9BFE\u0000\u0000\u0000\uD5BE\u0000\u0000\u0000\u0000" + // 17300 - 17309
                "\uA1D8\u0000\u0000\u0083\u0000\uA1AE\uA1AF\u0000\u0000\uA1B0" + // 17310 - 17319
                "\uA1B1\u0000\u0000\uF7A2\u0000\u0000\u0000\u0000\u0000\u0000" + // 17320 - 17329
                "\uD4FE\u0000\u0000\uCDB2\u0000\uDAAB\u0000\uCAA7\u0000\u0000" + // 17330 - 17339
                "\uEAAC\u0000\u0000\u0000\uCAA8\u0000\u0000\uF3DD\u0000\u0000" + // 17340 - 17349
                "\u0000\uE4DA\u0000\u0000\uFDAA\uF9E2\u0000\u0000\u0000\u0000" + // 17350 - 17359
                "\u0000\u9DDD\uD8EA\u0000\u0000\uEAE7\uDFC3\uD1D2\uCEE2\u0000" + // 17360 - 17369
                "\uD3A4\u0000\uC8F0\u0000\u0000\uC8F1\uA0FE\u0000\u0000\uC7F3" + // 17370 - 17379
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF6DD\u0000\uF1A3\u0000" + // 17380 - 17389
                "\uC8F6\u0000\u0000\u0000\u0000\u0000\u0000\uCEBC\u0000\u0000" + // 17390 - 17399
                "\u0000\uD8F5\u0000\u0000\u0000\uCCCE\u0000\u0000\uC8AC\u0000" + // 17400 - 17409
                "\u0000\uC8AD\uC8AE\u0000\u0000\uC5B7\u0000\u0000\u0000\u0000" + // 17410 - 17419
                "\u0000\u0000\u9BA7\uF1CA\u0000\uCEA3\uB0F1\u9EB2\uB0F2\u0000" + // 17420 - 17429
                "\uB0F3\u0000\u0000\uB0F4\uB0ED\uB0EE\u0000\u0000\uB0EF\u0000" + // 17430 - 17439
                "\u0000\uB0F0\uB0E9\u0000\u0000\u0000\uB0EA\u0000\u0000\u0000" + // 17440 - 17449
                "\uCAF2\uDFA4\u0000\u0000\uD4C4\u0000\uF4B7\uFDC2\uFCB0\u0000" + // 17450 - 17459
                "\uFDEC\uCAE2\u0000\u0000\uD7C4\u0000\u0000\u0000\u0000\u0000" + // 17460 - 17469
                "\u0000\uE7E2\u0000\u0000\uDDDA\u0000\u0000\u0000\u0000\u0000" + // 17470 - 17479
                "\u0000\uE3CE\u0000\u0000\uE3A1\u0000\u0000\uE8E3\u0000\u0000" + // 17480 - 17489
                "\uF3AB\uB0E2\uB0E3\u0000\uB0E4\uB0E5\uB0E6\u0000\u0000\uEEC9" + // 17490 - 17499
                "\u0000\u0000\u0000\uE2DD\u0000\u0000\uE9E0\u0000\u9BB0\u0000" + // 17500 - 17509
                "\uD0D8\uFCA2\uD4BE\uB0E1\u0000\u0000\u0000\u0000\u0000\u0000" + // 17510 - 17519
                "\u0000\uE4E1\uB0DC\uB0DD\uB0DE\u0000\uB0DF\u0000\u0000\uB0E0" + // 17520 - 17529
                "\uB0DA\uB0DB\u0000\u0000\u0000\u0000\u0000\u0000\uDDC9\u0000" + // 17530 - 17539
                "\u0000\uD4D3\uB0D5\u0000\u0000\u9EB1\uB0D6\u0000\u0000\u0000" + // 17540 - 17549
                "\uF7EC\u0000\u0000\u0000\uE8F6\u0000\uCBD3\u0000\u0000\u9AD7" + // 17550 - 17559
                "\uE0BC\u0000\uF4CA\uD4FA\uB0CB\uB0CC\u0000\uB0CD\uB0CE\uB0CF" + // 17560 - 17569
                "\uB0D0\u9EB0\uB0C9\u9EAF\uB0CA\u0000\u0000\u0000\u0000\u0000" + // 17570 - 17579
                "\uBCD2\uBCD3\uBCD4\u0000\uD3D0\u0000\u0000\u0000\u0000\u0000" + // 17580 - 17589
                "\u0000\u0000\uDFC1\uB0C5\uB0C6\u0000\u0000\uB0C7\u0000\u0000" + // 17590 - 17599
                "\uB0C8\uB0C3\u0000\u0000\u0000\uB0C4\u0000\u0000\u0000\uCCAA" + // 17600 - 17609
                "\u0000\u0000\uF0C3\uCCD6\u0000\uF4FA\u0000\u0000\u0000\u0000" + // 17610 - 17619
                "\uCDD6\uFCF6\u0000\uA1A9\u0000\u0000\u0000\uA1AA\u0000\u0000" + // 17620 - 17629
                "\u0000\uDBF2\u0000\u0000\u0000\u0000\uC5F5\uC5F6\u0000\u0000" + // 17630 - 17639
                "\uDDC3\u0000\uF9DF\u0000\u0000\u0000\u0000\uC1DC\uC1DD\u0000" + // 17640 - 17649
                "\uC1DE\uB0BF\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uEDFE" + // 17650 - 17659
                "\uB0BC\uB0BD\u0000\u0000\uB0BE\u0000\u0000\u0000\uD2B5\u0000" + // 17660 - 17669
                "\u0000\u0000\uD3D5\u0000\uF9EB\uEEA3\u0000\u0000\u0000\u0000" + // 17670 - 17679
                "\u0000\u0000\uD1BE\u0000\u0000\uF6B9\u0000\u0000\u0000\u0000" + // 17680 - 17689
                "\u0000\u0000\uE9B3\u0000\u0000\uCDDF\u0000\u0000\uF5CB\u0000" + // 17690 - 17699
                "\uE4F0\uCBAB\uB0BA\uB0BB\u0000\u0000\u0000\u0000\u0000\u0000" + // 17700 - 17709
                "\uE3E5\u0000\uCBC5\uEAB4\uB0B5\u0000\u0000\u0000\uB0B6\u0000" + // 17710 - 17719
                "\u0000\u0000\uFDCD\u0000\u0000\u0000\uF3B6\u0000\uE4EE\uF9A1" + // 17720 - 17729
                "\u0000\u0000\uFBEF\u0000\u0000\u0000\uEFA8\u0000\u0000\u0000" + // 17730 - 17739
                "\uEEB4\uB0A8\uB0A9\uB0AA\uB0AB\uB0AC\uB0AD\uB0AE\uB0AF\uB0A5" + // 17740 - 17749
                "\uB0A6\uB0A7\u9EAE\u0000\u0000\u0000\u0000\uE0C1\uEFDB\u0000" + // 17750 - 17759
                "\u0000\uF0E9\uB0A1\uB0A2\u9EAD\u0000\uB0A3\u0000\u0000\uB0A4" + // 17760 - 17769
                "\uDBC2\u0000\u0000\u0000\u0000\uCAFE\u0000\u0000\uF4EA\u0000" + // 17770 - 17779
                "\u0000\u0000\uCEB9\u0000\u0000\uD4AA\u0000\uE5CC\u0000\u0000" + // 17780 - 17789
                "\u0000\u0000\uC8B8\uC8B9\u0000\u0000\uF7E5\u0000\u0000\u0000" + // 17790 - 17799
                "\uCCB2\u0000\u0000\uD3BF\u0000\u0000\u0000\u0000\u0000\u0000" + // 17800 - 17809
                "\uF0E7\uE2CC\u0000\uF9E0\u0000\u0000\u0000\u0000\uECD6\u0000" + // 17810 - 17819
                "\u0000\uD3E0\u0000\uE4BF\u0000\uFBC0\u0000\uDABE\uE0A9\u0000" + // 17820 - 17829
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCCC6\uDCAF\u0000\u0000" + // 17830 - 17839
                "\u0000\u0000\u0000\uF0A3\u0000\uEDAA\u0000\u0000\uF2A1\uCEE1" + // 17840 - 17849
                "\u0000\u0000\u0000\uD4AB\uCAB3\uCDA6\u0000\uCDC3\uD3DA\u0000" + // 17850 - 17859
                "\u0000\u0000\u0000\u0000\u0000\u9CC5\uD9F9\u0000\u0000\uD3EA" + // 17860 - 17869
                "\uF5F5\u9CEC\uEFC7\u0000\uDCFB\u0000\u0000\u0000\u0000\u0000" + // 17870 - 17879
                "\u0000\u0000\uF8B5\uFDD3\uEBED\uD6DC\u0000\u0000\u0000\u0000" + // 17880 - 17889
                "\u0000\uBCB6\uBCB7\u0000\uBCB8\uCDDC\uD9F7\u0000\u0000\u0000" + // 17890 - 17899
                "\u0000\u0000\u0000\u9DE4\uF3A3\u0000\uD3EC\uE4E5\u0000\u0000" + // 17900 - 17909
                "\u0000\u0000\u0000\u0000\u0000\uE2F5\u9CC0\uE4BC\u0000\u0000" + // 17910 - 17919
                "\u0000\u0000\u0000\u0000\uFCC6\u0000\u0000\u0000\uDBC9\u0000" + // 17920 - 17929
                "\u0000\u0000\uE4FA\u0000\uEEBA\u0000\u0000\u0000\u9AE6\u0000" + // 17930 - 17939
                "\uF8D3\u0000\uACEA\uACEB\uACEC\uACED\uACEE\uACEF\uACF0\uACF1" + // 17940 - 17949
                "\uE4CA\u0000\uDCE1\u9BFD\u0000\uF9C8\u0000\u0000\uD7E9\uEDF6" + // 17950 - 17959
                "\u0000\u0000\u0000\uDEED\u0000\uF1B2\u0000\uF1B1\u0000\u0000" + // 17960 - 17969
                "\u0000\u0000\u0000\uF9CD\u0000\u0000\u0000\uECB9\u0000\u0000" + // 17970 - 17979
                "\u0000\u0000\uC8D1\uC8D2\u0000\u0000\uE4DB\u0000\uE1FB\uCBA2" + // 17980 - 17989
                "\u0000\u0000\u0000\uE9E3\u0000\uEDCB\uCFE4\u0000\uACE2\uACE3" + // 17990 - 17999
                "\uACE4\uACE5\uACE6\uACE7\uACE8\uACE9\uCCF4\u0000\u0000\u0000" + // 18000 - 18009
                "\u0000\u0000\u0000\u0000\uD4F8\u9BB9\u0000\uE2D1\u0000\u0000" + // 18010 - 18019
                "\u0000\u0000\u9EA4\uCDD4\u0000\u0000\u0000\u0000\u0000\u0000" + // 18020 - 18029
                "\u0000\uE6E3\u9BEC\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 18030 - 18039
                "\uCDAC\uFAB5\u0000\u0000\u0000\u9AB1\u0000\u0000\u0000\uD3BC" + // 18040 - 18049
                "\u0000\u0000\u0000\uCAF0\u0000\uEFD0\u0000\uCDB1\u0000\u0000" + // 18050 - 18059
                "\u0000\u0000\u0000\uDDDF\u0000\u0000\u0000\uDAF2\u0000\u0000" + // 18060 - 18069
                "\u0000\u0000\uA0F9\uC8A2\u0000\u0000\uF6A6\u0000\u0000\u0000" + // 18070 - 18079
                "\u0000\u0000\u0000\uEBD7\u0000\u0000\uE0DA\u0000\u0000\u0000" + // 18080 - 18089
                "\uEEF7\u0000\u0000\uDFA3\u0000\u0000\u0000\u0000\u0000\u0000" + // 18090 - 18099
                "\u9BB1\u0000\u0000\uFDDF\u0000\u0000\u0000\u0000\u0000\u0000" + // 18100 - 18109
                "\uE3B2\u0000\u0000\uCBAA\u0000\u0000\u0000\u0000\u9BB4\u0000" + // 18110 - 18119
                "\uACDA\uACDB\uACDC\uACDD\uACDE\uACDF\uACE0\uACE1\uCDE9\u0000" + // 18120 - 18129
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCDDB\uD8E9\u0000\u0000" + // 18130 - 18139
                "\uF8FE\u0000\uCFCC\u0000\u0000\uE2BC\u0000\u0000\uFCED\uECE0" + // 18140 - 18149
                "\uD2FE\u0000\uFDE5\uF6A3\u0000\uD9FC\uFDA9\u0000\uE7EE\u0000" + // 18150 - 18159
                "\uACD1\uACD2\uACD3\uACD4\uACD5\uACD6\uACD8\uACD9\uD4F9\u0000" + // 18160 - 18169
                "\u0000\u0000\u0000\u0000\uF5E2\uE1D3\uDCC0\u0000\u0000\u0000" + // 18170 - 18179
                "\u0000\u0000\uD1C8\uD1C9\uF1D2\uD2CC\uCFCB\u0000\u0000\uCABD" + // 18180 - 18189
                "\u0000\u0000\uD8C9\u0000\u0000\u0000\u0000\u0000\u0000\uA9E7" + // 18190 - 18199
                "\uA9E8\uA9E9\uA9EA\uFBB0\u0000\u0000\u0000\uD8A9\uE5DF\uF9A7" + // 18200 - 18209
                "\u0000\uF8C5\u0000\u0000\u0000\u0000\u0000\uDCFA\u0000\uACBA" + // 18210 - 18219
                "\uACBB\uACBC\uACBD\uACBE\uACBF\uACC0\uACC1\uCEBD\u0000\u0000" + // 18220 - 18229
                "\u0000\u0000\u0000\u0000\u0000\uE0A4\uDCBF\u0000\u0000\u0000" + // 18230 - 18239
                "\u0000\u0000\u0000\u0000\uD0DC\uE6AE\u0000\u0000\u0000\u0000" + // 18240 - 18249
                "\u0000\uEFB6\u0000\uF7CE\uFABE\u0000\u0000\u0000\u0000\u0000" + // 18250 - 18259
                "\u0000\uF5C5\uEEE0\u0000\uACB2\uACB3\uACB4\uACB5\uACB6\uACB7" + // 18260 - 18269
                "\uACB8\uACB9\uF3C9\u0000\u0000\uE4BB\u0000\u0000\u0000\u0000" + // 18270 - 18279
                "\u9ADF\uD6BB\uDED6\u0000\u0000\uECBE\u0000\u0000\u0000\uE5B4" + // 18280 - 18289
                "\uCDC8\uEEC8\uF9A6\u0000\u0000\u0000\u0000\u0000\u0000\uDFBD" + // 18290 - 18299
                "\u9BF0\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uDCDC\uEAC3" + // 18300 - 18309
                "\u0000\uEFB4\u0000\u0000\u0000\uD7BE\u0000\uF9EA\uD1CE\uEED4" + // 18310 - 18319
                "\u0000\uD4D2\uD9A3\uFDA8\uD7D9\uCCF2\uF7DD\u0000\uDEBA\u0000" + // 18320 - 18329
                "\u0000\u0000\u0000\uF6E1\u0000\u0000\u0000\u0000\uC3BC\uC3BD" + // 18330 - 18339
                "\u0000\u0000\uEABD\uE6FE\u9AFB\uF7C4\uF5AD\u0000\uD9E0\uFAFA" + // 18340 - 18349
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD7EA\uD6C5\u0000" + // 18350 - 18359
                "\u0000\u0000\u0000\u0000\u0000\u0000\uD5C5\uE7E8\uE8D7\uDAF8" + // 18360 - 18369
                "\uD4CB\u0000\u0000\u0000\uF7F6\uE2CE\u0000\uE9F5\u0000\uE1EB" + // 18370 - 18379
                "\u0000\u0000\u0000\uFCE1\uEDB0\uFDD1\uF6BB\u0000\u0000\uD0D9" + // 18380 - 18389
                "\u0000\u0000\uDDD2\uF7F4\uE7DC\uE4A5\uFBE1\uFAED\uF0A2\uCCF1" + // 18390 - 18399
                "\u0000\uFAA3\uE2F7\u0000\uDEC9\u0000\u0000\u0000\u0000\u0000" + // 18400 - 18409
                "\u0000\u0000\uDBD7\uCAEA\u0000\u0000\uCFD4\u0000\uF8BD\u0000" + // 18410 - 18419
                "\u0000\uDDB3\uD4EC\u0000\u0000\uF2B9\u0000\uDFB7\uCFD3\u0000" + // 18420 - 18429
                "\u0000\u0000\u0000\u0000\u9DDB\u0000\uF7BB\uF2EA\uDEC8\uE9D3" + // 18430 - 18439
                "\u0000\u0000\u0000\u0000\uC1D6\uC1D7\u0000\u0000\uDFC6\u0000" + // 18440 - 18449
                "\u0000\u0000\u0000\u0000\u0000\uE3DA\u0000\uFCD9\u9DD1\u0000" + // 18450 - 18459
                "\u0000\u0000\u0000\u0000\u0000\u0000\uD9AD\uD6C4\u9CCA\u0000" + // 18460 - 18469
                "\u0000\u0000\u0000\u0000\u0000\uE5F2\u0000\u0000\uD0F4\uDFAC" + // 18470 - 18479
                "\u0000\uD6DA\u0000\u0000\u0000\u0000\u0000\uBBCE\u0000\u0000" + // 18480 - 18489
                "\u0000\uFCE0\uD7C8\uFDAD\u0000\u0000\u0000\uECE2\u0000\u9CFC" + // 18490 - 18499
                "\u0000\u0000\uF3EC\u0000\u0000\u0000\u0000\uDEA1\u0000\uE9D1" + // 18500 - 18509
                "\uF3A9\uD0E0\uE9D2\u0000\uDAE3\u0000\u0000\uE1B4\u0000\u0000" + // 18510 - 18519
                "\u0000\u0000\uF4D3\u0000\uACAA\uACAB\uACAC\uACAD\uACAE\uACAF" + // 18520 - 18529
                "\uACB0\uACB1\uE2CD\u0000\u0000\u0000\u9CAE\u0000\uEFFD\uF2E8" + // 18530 - 18539
                "\uDDD4\u0000\uEAA3\u0000\u0000\u0000\uD6C3\uD6F4\uE9EB\uE9EC" + // 18540 - 18549
                "\uE0E4\u0000\u0000\u0000\u0000\uDAA7\uEDCD\uE4D2\u0000\u0000" + // 18550 - 18559
                "\uEAA9\uE4BA\uF3A2\uCDD2\uE2CB\u0000\uFACF\u0000\u0000\u0000" + // 18560 - 18569
                "\u0000\u0000\u9FC9\u0000\u0000\u0000\uFBA1\u0000\u0000\u0000" + // 18570 - 18579
                "\uE5E9\uE9EE\uE4F6\uD0C0\u0000\uF0B7\uEEA1\u0000\u0000\u0000" + // 18580 - 18589
                "\uCBAD\u0000\uF9B0\u0000\u0000\u0000\uE9FE\u0000\u0000\u0000" + // 18590 - 18599
                "\u0000\uE4ED\u0000\u0000\u0000\u0000\uA0AF\uC1C5\u0000\uC1C6" + // 18600 - 18609
                "\uD7C1\u0000\u0000\u0000\u0000\uE5D5\u0000\u0000\uE2BB\u0000" + // 18610 - 18619
                "\uF7AD\u0000\u0000\u0000\uF8E1\uEBE4\u0000\u0000\uF2E7\u0000" + // 18620 - 18629
                "\uD7D5\uD4B6\uF9E8\uF9DA\u0000\u0000\u0000\u0000\u0000\u0000" + // 18630 - 18639
                "\u0000\uFDD0\uF6ED\u0000\uF9AE\u0000\uDDBE\u0000\u0000\u0000" + // 18640 - 18649
                "\uF1B7\uEEF8\u0000\u0000\u0000\uD9D9\u9CCB\u0000\uF8A1\u0000" + // 18650 - 18659
                "\u0000\u0000\uE8D6\u0000\uF6B2\u0000\u0000\u0000\u0000\uCFF0" + // 18660 - 18669
                "\uF9BD\u0000\uACA1\uACA2\uACA3\uACA4\uACA5\uACA6\uACA8\uACA9" + // 18670 - 18679
                "\uD0B1\u9BC5\u0000\u0000\u0000\uD5EF\u0000\u0000\uCEDD\uEBC0" + // 18680 - 18689
                "\u0000\uFDA2\u0000\u0000\u0000\uEEED\u0000\u0000\u0000\uECEB" + // 18690 - 18699
                "\uDEC5\uCBA6\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD5A1" + // 18700 - 18709
                "\uDAA6\u0000\u0000\uE0EC\u0000\u0000\u0000\u0000\uD3F7\u0000" + // 18710 - 18719
                "\u0000\u0000\u0000\uC2EB\uA0C3\u0000\uC2EC\u9BF8\u0000\u9AF6" + // 18720 - 18729
                "\u0000\u0000\u0000\u0000\u0000\uBBAD\uBBAE\u0000\uBBAF\uF7A1" + // 18730 - 18739
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uEECB\uF1A4\u9AEC" + // 18740 - 18749
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF5CD\u0000\uF1F2\uFAC7" + // 18750 - 18759
                "\uECF0\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uEBFB\uE7CC" + // 18760 - 18769
                "\u0000\uD6A8\uCEA7\u0000\uD4B5\u0000\u0000\uCCB7\uDBB8\u0000" + // 18770 - 18779
                "\u0000\u0000\u0000\uD0E9\uD9E1\u0000\u0000\uE0B8\u0000\u0000" + // 18780 - 18789
                "\uCDD1\uF3B9\uEFFC\uD1C4\uEFB1\u0000\uD1C5\u0000\uD0DE\u0000" + // 18790 - 18799
                "\uD7D8\u0000\uFDA7\u0000\u0000\u0000\u0000\uEAAB\uF5DF\u0000" + // 18800 - 18809
                "\uEEB6\u0000\u0000\u0000\uE2F6\uD3CA\uF5DE\u0000\u0000\u0000" + // 18810 - 18819
                "\u0000\u0000\u0000\u0000\uD9AB\uCBEA\u0000\u0000\u0000\uCBBC" + // 18820 - 18829
                "\u0000\u0000\u0000\uE5F5\u0000\u0000\u0000\u0000\u0000\uB1D4" + // 18830 - 18839
                "\u0000\u0000\u0000\uD4E5\u0000\u0000\u0000\uF9C3\uD9AF\u0000" + // 18840 - 18849
                "\u0000\u0000\uF9E7\u0000\u0000\u0000\uDEAE\u0000\u0000\u0000" + // 18850 - 18859
                "\u0000\u0000\uB1AB\uB1AC\u0000\u0000\uCDC6\uF2B6\u0000\u0000" + // 18860 - 18869
                "\uDDFE\u0000\u0000\uD4A9\u0000\u0000\u0000\u0000\uCDC2\uE7DA" + // 18870 - 18879
                "\uEBDE\u0000\u0000\uF5C8\u0000\uD4DE\u0000\u0000\uEBBF\u0000" + // 18880 - 18889
                "\uD7CE\uD1BF\u0000\u0000\u0000\uD0D1\uCBBF\u0000\uEDA4\u0000" + // 18890 - 18899
                "\u0000\uFADF\u0000\u0000\u0000\u0000\u0000\u0000\uE4BD\u0000" + // 18900 - 18909
                "\u0000\uDBE1\u0000\u0000\uE5C9\u0000\uEDB4\u0000\uECD4\uEACB" + // 18910 - 18919
                "\u0000\u0000\uCABF\uD5B0\u0000\uCFE9\u9AC4\u0000\u0000\u0000" + // 18920 - 18929
                "\u0000\u0000\u9BC0\u0000\uE0D9\u0000\u0000\u0000\u0000\u0000" + // 18930 - 18939
                "\u0000\uD9D6\uCBA5\u0000\u0000\u0000\u0000\uCBE9\u0000\u0000" + // 18940 - 18949
                "\uCEEE\u0000\u9BCD\uECCF\u0000\u0000\u0000\uE7D1\uD2AC\u0000" + // 18950 - 18959
                "\uCEF9\u0000\u0000\uE0FD\u0000\u0000\uD8F8\u0000\u0000\u0000" + // 18960 - 18969
                "\uBDD7\uBDD8\uBDD9\u0000\u0000\uD6A1\uFDBF\u0000\uFCD3\u0000" + // 18970 - 18979
                "\uEFA1\u0000\uEFBF\u0000\u0000\u0000\u0000\u0000\uCECF\u0000" + // 18980 - 18989
                "\uA5F7\uA5F8\u0000\u0000\u0000\u0000\u0000\u0000\uCCDD\u0000" + // 18990 - 18999
                "\u0000\u9CE4\u0000\uFDDE\uCAC0\u0000\u0000\u0000\uD9C3\uD0E8" + // 19000 - 19009
                "\u0000\u0000\u0000\uE0B4\u0000\u0000\u0000\u0000\uC7FD\u0000" + // 19010 - 19019
                "\u0000\u0000\uD7BC\uCCE3\u0000\u0000\uE6DB\uCCA2\uF7FE\uDFBC" + // 19020 - 19029
                "\u0000\u9DD0\u0000\u0000\uEBCD\uEFF9\u0000\u0000\u0000\uDDBC" + // 19030 - 19039
                "\uF6DC\u0000\u0000\uF8BB\u0000\uE8D1\u0000\u0000\u0000\u0000" + // 19040 - 19049
                "\uECF2\u0000\u0000\u0000\u0000\uC0CC\uC0CD\u0000\u0000\uF2D7" + // 19050 - 19059
                "\u0000\uCAF8\uDAEF\u9AC2\u0000\uD6D4\uD7ED\uD1D1\u0000\u0000" + // 19060 - 19069
                "\u0000\u0000\u0000\uE1F2\uE5D4\u0000\u0000\u0000\u0000\u0000" + // 19070 - 19079
                "\u0000\uF3FA\u9DFD\u0000\uE1A5\u0000\u0000\u0000\u0000\u0000" + // 19080 - 19089
                "\uBAC4\u0000\u0000\u0000\uD8DA\u9EA5\u0000\u0000\u0000\u0000" + // 19090 - 19099
                "\uC2D6\u0000\u0000\u0000\uECBC\u0000\u0000\uE5AD\u0000\uE7ED" + // 19100 - 19109
                "\uFDC1\uDAE2\u0000\u0000\uD8B3\u0000\u0000\uDCE6\u0000\u0000" + // 19110 - 19119
                "\uDED2\u0000\u0000\uEDE2\uDFAB\u0000\u0000\u0000\u0000\u0000" + // 19120 - 19129
                "\u0000\u0000\uD6A4\uDDBB\u0000\u0000\u0000\u0000\uCEAC\u0000" + // 19130 - 19139
                "\u0000\uE6BA\u0000\u0000\uCDA9\u0000\u0000\u0000\uA1F8\uA1F9" + // 19140 - 19149
                "\u0000\u0000\uA1F6\uA1F7\uEED0\u0000\u0000\u0000\u0000\u0000" + // 19150 - 19159
                "\u0000\u0000\uEEE1\uF7C6\uCFC8\u0000\u0000\u0000\uE1D0\u0000" + // 19160 - 19169
                "\u0000\uE3B1\uFCEB\uCDA8\u0000\uCCB6\u0000\u0000\uEBF7\u0000" + // 19170 - 19179
                "\u0000\u0000\u0000\u0000\u0000\uF0E8\u0000\uDDC0\uF5BE\u0000" + // 19180 - 19189
                "\uDEF7\u0000\u0000\u0000\u0000\uCAFB\uD8B1\u0000\uDCAB\u0000" + // 19190 - 19199
                "\u0000\u0000\u0000\uD5A4\uE9AD\uD8E4\uFAB3\uE2C5\uFCBD\u0000" + // 19200 - 19209
                "\u0000\uECC4\uE0D4\u0000\uEBB6\u0000\uD7A1\uCBE8\u0000\uF9AD" + // 19210 - 19219
                "\u9CDD\uEEEA\u0000\u0000\u0000\uF0E4\uF3B4\uD4EE\uEAC0\uE1CF" + // 19220 - 19229
                "\u0000\uCCBA\u0000\u0000\u0000\u0000\u9BE5\u0000\uF6E6\u0000" + // 19230 - 19239
                "\u0000\uDBE5\u0000\uDDDE\u0000\u0000\uD9F0\uE9A3\uF9C6\uFCDA" + // 19240 - 19249
                "\u0000\uD4B3\uD3B9\uEADE\u0000\u0000\uF0FD\u0000\u0000\u0000" + // 19250 - 19259
                "\u0000\u0000\uD7AC\uECEF\u0000\u0000\u0000\uF9BA\u0000\uEBB5" + // 19260 - 19269
                "\u0000\uCFA1\uE4A8\u0000\uF4B6\uECFE\u0000\u0000\uE3AE\uF0E3" + // 19270 - 19279
                "\uF1E4\uDCF1\uD6A7\u0000\u0000\u0000\u0000\uD0F5\u0000\u0000" + // 19280 - 19289
                "\uE8ED\uD0D2\uF5EF\uCFC7\u0000\u0000\uD4B2\uCCEF\u0000\uD4E8" + // 19290 - 19299
                "\uFBAD\u0000\u0000\uF8E7\u0000\uE1CE\u0000\uF7E2\uF7DC\uE1EA" + // 19300 - 19309
                "\uCEC1\uD4B1\u0000\uFDB1\uE6BD\u0000\uEDDD\uCEC4\u0000\uCBA1" + // 19310 - 19319
                "\u0000\u0000\u0000\u0000\uC1B5\u0000\u0000\u0000\uF4D7\uCCA1" + // 19320 - 19329
                "\u9ABB\u0000\uCFBA\u9BD4\uEEE9\u9AD9\u0000\u0000\uF5DA\u0000" + // 19330 - 19339
                "\u0000\uF2DB\uE4FC\u0000\u0000\u0000\u0000\u0000\uB6CF\u0000" + // 19340 - 19349
                "\u0000\u0000\uA1EC\uA1ED\u0000\u0000\u0000\u0000\uBFE4\uBFE5" + // 19350 - 19359
                "\u0000\u0000\uF1F7\u0000\u0000\u0000\uE8B8\u0000\u0000\uDEB4" + // 19360 - 19369
                "\u0000\u0000\u0000\u0000\u0000\u0000\uFBE2\u0000\uCDD3\uE2FB" + // 19370 - 19379
                "\u0000\uCCA6\u0000\u0000\u0000\u0000\uDABB\uF2E3\uE9B4\uD2DC" + // 19380 - 19389
                "\u0000\u0000\u0000\u0000\u0000\uB9F6\uB9F7\u0000\u0000\uCBB5" + // 19390 - 19399
                "\uD8D1\u0000\uF4CE\uF3F7\u0000\u0000\uDCBA\u0000\u9DD9\uCCB4" + // 19400 - 19409
                "\u0000\u0000\u0000\uB2FA\uB2FB\uB2FC\u0000\uB2FD\uDCA9\u0000" + // 19410 - 19419
                "\u0000\u0000\u0000\uDEF6\u9BD0\uDCAA\uE2C3\uDCDE\u0000\uDCDF" + // 19420 - 19429
                "\u0000\u0000\uEFAD\uE6AB\uF5EE\u0000\u0000\uCABB\u0000\u0000" + // 19430 - 19439
                "\uE3DC\u0000\uDCD3\u0000\u0000\u0000\u0000\uDDE2\uFBF9\uDDC1" + // 19440 - 19449
                "\uCFC6\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF6C5\uF4B2" + // 19450 - 19459
                "\u0000\u0000\u0000\u9DBA\u0000\u0000\u0000\uE7F2\uEDDF\u0000" + // 19460 - 19469
                "\u0000\uCACB\u0000\uFDD6\u0000\u0000\u0000\u0000\uF8D1\u0000" + // 19470 - 19479
                "\uF8D2\uD4B0\uF3B2\uFBB7\u0000\u0000\u0000\u0000\u0000\uB9E3" + // 19480 - 19489
                "\uB9E4\u0000\uB9E5\uEBB2\u0000\u0000\u0000\u0000\uF1A2\u9DB0" + // 19490 - 19499
                "\u0000\uCFE8\u0000\uEDC3\uD0B2\u0000\u0000\uCEFE\uDAA8\uF4C2" + // 19500 - 19509
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u9AD6\uCEA5\u0000" + // 19510 - 19519
                "\u9ACE\u0000\u0000\u0000\u0000\uD6D8\uF5D9\u0000\u0000\u0000" + // 19520 - 19529
                "\u0000\u0000\u0000\u0000\u9BA5\uF1CB\u0000\u0000\uD0AF\uDDB9" + // 19530 - 19539
                "\u0000\u0000\uD1C3\uF1FC\u0000\uF3C7\u9CBF\u0000\uE0EB\u0000" + // 19540 - 19549
                "\u0000\uCCB5\u0000\u0000\u0000\u0000\u0000\uCFBD\uDBD3\u0000" + // 19550 - 19559
                "\uFAE7\uD8E3\uF4C1\u0000\uDDB7\u0000\uCAEB\uD9E2\u0000\uFDB2" + // 19560 - 19569
                "\u0000\uE3AD\uD6CC\uD9B4\uCAB9\u0000\uEEE4\u0000\u0000\u0000" + // 19570 - 19579
                "\u0000\u0000\uB9C2\u0000\u0000\u0000\uE1F7\u0000\u0000\u0000" + // 19580 - 19589
                "\u0000\u0000\uDABD\u0000\u0000\u0000\uDAB7\u0000\u0000\u0000" + // 19590 - 19599
                "\u0000\uA1C9\u0000\u0000\u0000\u0000\uBFD6\uBFD7\u0000\u0000" + // 19600 - 19609
                "\uF7D6\uDEEA\uCBB4\u0000\u0000\uEFBE\u0000\uCAA5\u0000\u0000" + // 19610 - 19619
                "\uD6AB\uD0C2\u0000\u0000\u0000\uEFEC\u0000\u0000\u0000\u0000" + // 19620 - 19629
                "\uC7DC\uC7DD\u0000\uC7DE\uF9C5\uDDD3\uD6F1\uECFC\uFCF0\u0000" + // 19630 - 19639
                "\u0000\uEDC0\uD3E8\u0000\u0000\uDEA8\uF4E4\uECC2\u0000\uD9F5" + // 19640 - 19649
                "\uE1AE\u0000\u0000\uECC3\uCFFE\u0000\uF8BF\uD8E2\uFCA7\uF7FC" + // 19650 - 19659
                "\uF7B1\uCEBB\uF4A1\u0000\u0000\uEECD\uDDB6\uEEAF\uCDF8\u0000" + // 19660 - 19669
                "\u0000\u0000\u0000\uDEB8\uD1C2\u0000\uF9A5\u0000\uE8D5\u0000" + // 19670 - 19679
                "\u0000\u0000\uE1F6\uDECC\u0000\u0000\uFCDE\u0000\uDBF9\uD7B1" + // 19680 - 19689
                "\u0000\u0000\u0000\uCBFC\u0000\u0000\uCDFB\uF6D6\u0000\uE7F5" + // 19690 - 19699
                "\uE8EF\uE3F9\uD2BB\uE2C2\u0000\uF3D8\uE5D3\u0000\u0000\uF3D9" + // 19700 - 19709
                "\u0000\uCFE7\uF3CB\uEDA9\uCABE\u0000\u0000\u0000\u0000\uC0F7" + // 19710 - 19719
                "\u0000\u0000\u0000\uE0FC\uD4A8\u0000\uEDD3\uD8EF\uD4C1\u0000" + // 19720 - 19729
                "\u0000\u0000\u0000\u0000\u0000\u0000\u9DCA\uECA1\u0000\u0000" + // 19730 - 19739
                "\u0000\uCCB9\u0000\u0000\uFBDE\uE3DB\u0000\uD3C9\u0000\uDCCF" + // 19740 - 19749
                "\u0000\u0000\u0000\uE7F0\u0000\uD0EE\u0000\u0000\uF3AA\uD9C8" + // 19750 - 19759
                "\u0000\u0000\uEEE3\uD7BD\u0000\u0000\u0000\uF4C9\u0000\u0000" + // 19760 - 19769
                "\u0000\u0000\u0000\uCDAE\u0000\u0000\u0000\u9CCD\u0000\u0000" + // 19770 - 19779
                "\u0000\u0000\uC8E2\u0000\u0000\uC8E3\uE9AA\u0000\u0000\u0000" + // 19780 - 19789
                "\u0000\u0000\u0000\u0000\uCBCD\uDACD\u0000\u0000\u0000\uF9CC" + // 19790 - 19799
                "\u0000\uE1DA\uDBBF\uD9C7\uE4D7\uEADD\u0000\uD4F7\u0000\u0000" + // 19800 - 19809
                "\u0000\uD5E5\u0000\u0000\u0000\u0000\u0000\u9DE5\u0000\u0000" + // 19810 - 19819
                "\u0000\uCAE5\u0000\u0000\u0000\uDCA1\uF0B3\u0000\uE5EC\u0000" + // 19820 - 19829
                "\u0000\u0000\uD1E7\u0000\uD3F0\u9DC3\u0000\u0000\u0000\u0000" + // 19830 - 19839
                "\uF0A4\uE1EC\uE2C1\u0000\uCEA4\u0000\u0000\u0000\u0000\u0000" + // 19840 - 19849
                "\uB8EF\u0000\u0000\u0000\uDACF\u0000\uDCD4\u0000\uDCA6\u0000" + // 19850 - 19859
                "\uE7D4\u0000\uCACA\u0000\u0000\u0000\uD9FB\u0000\uA5F0\uA5F1" + // 19860 - 19869
                "\u0000\uA5F2\uA5F3\uA5F4\uA5F5\uA5F6\uFCEF\u0000\uE0E3\u0000" + // 19870 - 19879
                "\u0000\u0000\u0000\u0000\uB8E2\uB8E3\u0000\uB8E4\uE1A4\uCDAB" + // 19880 - 19889
                "\u0000\uD9F4\uE8A6\uCDCE\uE1E9\u0000\uD3EF\u0000\u0000\uECD3" + // 19890 - 19899
                "\u0000\u0000\uDDC2\uEFB7\uEBAF\u0000\u0000\u0000\u0000\u0000" + // 19900 - 19909
                "\uE5DE\u0000\uF4C8\uE8EA\uF5F3\u0000\u0000\uF9DE\u0000\u0000" + // 19910 - 19919
                "\uFAA9\u0000\uE1DD\u0000\u0000\u0000\u0000\uC2E8\u0000\u0000" + // 19920 - 19929
                "\u0000\uE3ED\u0000\uE8C2\u0000\uEDF5\uFDFE\uFCA5\uFAB1\uDFD9" + // 19930 - 19939
                "\u0000\uE0D2\u0000\u0000\uE2B6\u0000\u0000\u0000\u0000\uEFF1" + // 19940 - 19949
                "\u0000\uFCC5\uCBC2\u0000\u0000\u0000\u0000\uFDD5\u0000\uA5E8" + // 19950 - 19959
                "\uA5E9\uA5EA\uA5EB\uA5EC\uA5ED\uA5EE\uA5EF\uE7C9\u0000\uE2F3" + // 19960 - 19969
                "\uE7E1\u0000\u0000\uE3CB\u0000\uCEAE\u0000\u0000\u0000\u0000" + // 19970 - 19979
                "\uD9A2\u0000\u0000\uD2EC\u0000\u0000\u0000\u0000\u0000\u0000" + // 19980 - 19989
                "\uF2F4\u0000\u0000\uFDF0\u0000\uE0BD\uCEE3\u0000\u0000\u0000" + // 19990 - 19999
                "\uF0FE\u0000\u0000\u0000\u0000\u9BAC\uD7CF\uEBEA\uFDEB\u0000" + // 20000 - 20009
                "\uA5D7\uA5D8\u0000\u0000\u0000\u0000\u0000\u0000\uE3A5\u0000" + // 20010 - 20019
                "\u0000\uE7D5\uF5BF\uCFA2\uCDAF\uCFA3\u0000\u0000\uCDB0\uF1FE" + // 20020 - 20029
                "\uD0A3\uE1AF\uF8A3\u0000\uCAA6\uDEF1\u0000\u0000\u0000\uF0DF" + // 20030 - 20039
                "\uF8C4\u0000\u0000\uD5DC\uF3C4\uCBD7\u0000\u0000\u0000\u0000" + // 20040 - 20049
                "\uA8F7\uA8F8\u0000\u0000\u0000\u9AAB\u0000\u0000\u0000\u0000" + // 20050 - 20059
                "\u0000\uF0A1\u0000\uDEAA\u0000\uD0ED\u0000\u0000\u0000\u0000" + // 20060 - 20069
                "\u0000\uE5F7\u0000\uA5D0\uA5D1\u0000\uA5D2\uA5D3\uA5D4\uA5D5" + // 20070 - 20079
                "\uA5D6\uD1C0\u0000\u0000\uE8C5\u0000\uE4B8\u0000\uE1E8\uCDAA" + // 20080 - 20089
                "\u0000\uE3F2\u0000\uFBF7\u0000\uF7D0\u0000\uEEF0\u0000\u0000" + // 20090 - 20099
                "\u0000\uCCC2\u0000\u0000\u0000\uEAD4\u0000\u0000\u0000\u0000" + // 20100 - 20109
                "\uA0EB\u0000\u0000\u0000\uDFA7\u0000\uDFE7\uE1C1\u0000\uA5C8" + // 20110 - 20119
                "\uA5C9\uA5CA\uA5CB\uA5CC\uA5CD\uA5CE\uA5CF\uE5EB\u0000\uEFF4" + // 20120 - 20129
                "\uDDB5\u0000\u0000\u0000\u0000\uD0F0\u0000\u0000\u0000\u0000" + // 20130 - 20139
                "\uC2AB\uC2AC\u0000\uC2AD\uF5BA\u0000\u0000\u0000\u0000\u0000" + // 20140 - 20149
                "\u0000\u0000\uD9DF\uCEBA\u0000\u0000\u0000\u0000\u0000\u0000" + // 20150 - 20159
                "\u0000\uE2B4\uD7AE\u0000\u0000\uE0E1\u0000\u0000\u0000\u0000" + // 20160 - 20169
                "\uFAC6\u0000\u0000\u0000\u0000\uA0B9\u0000\u0000\u0000\uDEFB" + // 20170 - 20179
                "\uD0BB\uD5B7\uEEF1\u0000\u0000\uCADB\u0000\u0000\u0000\u0000" + // 20180 - 20189
                "\u0000\uFCD7\uEADC\uDBD2\u0000\u0000\u0000\u0000\u0000\u0000" + // 20190 - 20199
                "\uF4A8\u0000\uDCF8\u0000\uEEEF\uD5D7\uEAE4\uF8A2\uCDEB\uD7BF" + // 20200 - 20209
                "\uFBB1\u0000\uA2A8\uA2AB\uA2AA\uA2AD\uA2A6\uA2A9\u0000\u0000" + // 20210 - 20219
                "\uDDE4\uF0EF\uF6F1\uFAF0\u0000\u0000\uD1F5\uCAE8\u0000\uF8E6" + // 20220 - 20229
                "\uDCCE\u0000\u0000\u0000\u0000\uDECB\uF6B8\u0000\u0000\u0000" + // 20230 - 20239
                "\u9DE0\u0000\u0000\u0000\uE5A7\uD5D2\uD5A3\u0000\u0000\u0000" + // 20240 - 20249
                "\u0000\uF0B2\u0000\u0000\uDEE8\u0000\u0000\u0000\u0000\u0000" + // 20250 - 20259
                "\u0000\uA0DB\u0000\u0000\uD7B8\u0000\u0000\u0000\u0000\u0000" + // 20260 - 20269
                "\u0000\u9AD4\u9CE0\u0000\uE0BB\uCEC3\u0000\uD0BA\uF7BA\uD8F3" + // 20270 - 20279
                "\uF7CD\u0000\uA2B0\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 20280 - 20289
                "\u0000\uA2A7\uDEA5\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 20290 - 20299
                "\uE6DA\uCAB7\u0000\u0000\uD3E7\u0000\uF8E5\u0000\u0000\uCEB7" + // 20300 - 20309
                "\u0000\u0000\u0000\u0000\u0000\u0000\uC2C6\u0000\u0000\uE0C4" + // 20310 - 20319
                "\u0000\uCFB9\u0000\uD5CA\uD7E2\uE2AF\uE1F1\u0000\uD2A4\u0000" + // 20320 - 20329
                "\u0000\u0000\u0000\uF5FB\uF8FA\u0000\u0000\uDFB9\u0000\u0000" + // 20330 - 20339
                "\u0000\u0000\uF9E1\u0000\u0000\u0000\u0000\uC1ED\u0000\u0000" + // 20340 - 20349
                "\u0000\uD8F1\u0000\uD4CF\u0000\u0000\u0000\uE6B9\u0000\u0000" + // 20350 - 20359
                "\u0000\u0000\uC7B4\u0000\u0000\u0000\uD7AA\u0000\u0000\u0000" + // 20360 - 20369
                "\u0000\uC7C1\uA0EE\u0000\u0000\uE1AB\u0000\u0000\u0000\u0000" + // 20370 - 20379
                "\u0000\u0000\uBCD1\u0000\u0000\uE0FB\u0000\u0000\u0000\uEFEA" + // 20380 - 20389
                "\uFADE\u0000\uE8B4\uEBC3\u0000\uEAAA\uFAFC\uF5F6\uF0BC\uFDD4" + // 20390 - 20399
                "\uFAEC\u0000\u0000\u0000\u0000\u0000\uF1EB\u0000\uEBF0\uF1D6" + // 20400 - 20409
                "\u0000\u0000\uE5E2\u0000\uCCCC\u0000\uA9A8\uA8A9\uA9A9\u0000" + // 20410 - 20419
                "\u0000\u0000\u0000\u0000\uDDBD\u0000\u0000\u0000\uF9C1\u0000" + // 20420 - 20429
                "\u0000\u0000\u0000\uC5F0\u0000\u0000\u0000\uFBF2\u0000\uDBF6" + // 20430 - 20439
                "\u0000\uDEDF\uDAF6\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 20440 - 20449
                "\uF5B8\u9CAF\u0000\u0000\u0000\uF6DE\u0000\u0000\u9BB6\uE8C4" + // 20450 - 20459
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u9BC2\uE3A4\u0000" + // 20460 - 20469
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCFEF\u9BD7\u0000\u0000" + // 20470 - 20479
                "\u0000\u0000\u0000\uF9C4\u0000\uDFA1\uDDE1\u0000\u0000\u0000" + // 20480 - 20489
                "\u0000\u0000\u0000\uFCA1\uEFEE\uDCD8\uF2BB\u0000\uDEA4\u0000" + // 20490 - 20499
                "\uDACC\u0000\u0000\u0000\uF3FC\u0000\u0000\uEEA2\u0000\u0000" + // 20500 - 20509
                "\uE1C5\u0000\u0000\u0000\u0000\u0000\u0000\uB9A3\u0000\uB9A4" + // 20510 - 20519
                "\uE8A5\u9BDD\u0000\u0000\u0000\u0000\u0000\u0000\uF4A2\u0000" + // 20520 - 20529
                "\uF1D7\u0000\uCED3\u0000\u0000\u0000\u0000\uDCF7\u0000\u0000" + // 20530 - 20539
                "\uEED5\u0000\u0000\u0000\u0000\uF9F4\u0000\uA9A7\u0000\u0000" + // 20540 - 20549
                "\u0000\u0000\u0000\u0000\uA8A8\uF5B9\u0000\uDCF0\uE3F1\u0000" + // 20550 - 20559
                "\u0000\u0000\u0000\uCFD6\u0000\uD7F0\u0000\uEBE1\uF9CB\u0000" + // 20560 - 20569
                "\u0000\u0000\uCBF3\uF4A5\u0000\u0000\uF3D7\u0000\u0000\u0000" + // 20570 - 20579
                "\uDCBD\u0000\uCCE5\uFDB9\u0000\u0000\u0000\u0000\u0000\u0000" + // 20580 - 20589
                "\u0000\uD0B4\uFDBC\uDFB1\uE3EF\u0000\u0000\u0000\u0000\uE0A3" + // 20590 - 20599
                "\u9CDF\u0000\u0000\u0000\uDADD\u0000\u0000\uDAB9\uCFF2\uF7B9" + // 20600 - 20609
                "\uD9F3\u0000\u0000\uE1CB\u0000\u0000\uCFE2\uCDF6\u0000\u0000" + // 20610 - 20619
                "\uEFF0\u0000\uF4BE\u9CE1\uFBB6\u0000\u0000\u0000\u0000\u0000" + // 20620 - 20629
                "\u0000\uA1AD\u0000\u0000\u0000\uE9D0\u0000\u0000\u0000\u0000" + // 20630 - 20639
                "\u0000\u9BE3\u0000\u0000\u0000\uD5B4\u0000\u0000\u0000\u0000" + // 20640 - 20649
                "\uC6D9\uC6DA\u0000\u0000\uE8B1\u0000\uFCAE\u0000\u0000\u0000" + // 20650 - 20659
                "\u0000\uA0FD\u0000\u0000\u0000\uA0B7\uC1E1\u0000\u0000\u0000" + // 20660 - 20669
                "\uA0CC\uC3C6\uA0CD\u0000\u0000\uFADD\u0000\u0000\u0000\u0000" + // 20670 - 20679
                "\u0000\u0000\uFCBF\u0000\u0000\uE2AB\uF3E8\u0000\u0000\u0000" + // 20680 - 20689
                "\u0000\u0000\uB4BC\u0000\u0000\u0000\u9BDE\u0000\uD4E9\u0000" + // 20690 - 20699
                "\u0000\uE3FE\uD1AA\uE8AA\u0000\uEAB6\uF9FA\uE6CC\uDFB8\u0000" + // 20700 - 20709
                "\uEAA5\u0000\u0000\u0000\uD7AD\u9DBB\uE1E0\u0000\uD9AC\u0000" + // 20710 - 20719
                "\uF5EB\u0000\uE0B6\u0000\uF7DE\u0000\u0000\u0000\u0000\u0000" + // 20720 - 20729
                "\u0000\u0000\uA9FA\uF1FA\u9AB2\u0000\uE5B6\uF3EF\u0000\u0000" + // 20730 - 20739
                "\uFBDA\uE2BD\u0000\u0000\u0000\uE3C8\u0000\u0000\u0000\uD6F6" + // 20740 - 20749
                "\u0000\u0000\u0000\uEACA\u0000\u9CAA\u0000\u0000\u0000\uF6B0" + // 20750 - 20759
                "\uEFCF\uE9CF\u0000\uA9AA\u0000\u0000\u0000\u0000\u0000\uA9AD" + // 20760 - 20769
                "\u0000\uC8E4\u0000\u0000\u0000\u0000\u0000\u0000\uF1D1\u0000" + // 20770 - 20779
                "\u0000\u0000\uF0BE\uD2BD\uCCA4\u0000\u0000\u0000\u0000\uC1AA" + // 20780 - 20789
                "\uC1AB\u0000\uC1AC\uEBAD\u0000\u0000\u0000\u9CBE\uD5AA\u0000" + // 20790 - 20799
                "\u0000\uCEA1\uF5A9\u0000\u0000\uDDF9\u0000\u0000\uE2AD\u0000" + // 20800 - 20809
                "\u0000\u0000\u0000\u0000\u0000\uF5E3\u0000\u0000\uF2D1\u9CB4" + // 20810 - 20819
                "\u0000\u0000\u0000\uE9C1\u0000\uCCA7\uEAC9\u0000\u0000\u0000" + // 20820 - 20829
                "\u0000\u0000\uF8B6\uCDCA\uD7D4\uDEA3\u0000\uE4E0\u0000\u0000" + // 20830 - 20839
                "\u0000\uE7EC\uEEEE\u9AC6\uF3F0\u0000\uDFBF\uE3EE\u0000\u0000" + // 20840 - 20849
                "\u0000\u0000\u0000\uE8D4\u0000\uCBDA\u0000\uE7D2\uD7C3\uF6F0" + // 20850 - 20859
                "\uE8DE\u0000\u0000\uF2EC\u0000\u0000\uFAEE\u0000\u0000\u0000" + // 20860 - 20869
                "\uE4FD\u0000\u0000\uE3EC\u0000\uA9A3\u0000\u0000\u0000\u0000" + // 20870 - 20879
                "\u0000\u0000\uA1C0\uE2F0\u0000\u0000\u0000\u0000\u0000\u0000" + // 20880 - 20889
                "\uFABB\uE9C7\uE6AA\u0000\u0000\u0000\u0000\u0000\u0000\uA9CD" + // 20890 - 20899
                "\uA9CE\uA9CF\uA9D0\uEDBC\u0000\u0000\uD8D4\u0000\u0000\u0000" + // 20900 - 20909
                "\uDCDA\uE9FD\uD0CA\u0000\uF5D6\uD9C5\uE4B4\u0000\uEDA7\uF5AC" + // 20910 - 20919
                "\u0000\u0000\u0000\u0000\u0000\uE4F5\u0000\uDCE4\u0000\uE5EF" + // 20920 - 20929
                "\u0000\u0000\u0000\u0000\u0000\uDEF8\uF8E9\uE3DE\u0000\uA8AA" + // 20930 - 20939
                "\u0000\u0000\u0000\u0000\u0000\uA8AD\uA9AC\u9CAD\uF3ED\u0000" + // 20940 - 20949
                "\u0000\u0000\u0000\u0000\u0000\uA9F9\u0000\u0000\u0000\uDCB1" + // 20950 - 20959
                "\u0000\u0000\u0000\uD5D6\u0000\uFAEF\uE3E1\u0000\u0000\u0000" + // 20960 - 20969
                "\u0000\u0000\u0000\uF5A6\u0000\u0000\uE8C6\u0000\u0000\u0000" + // 20970 - 20979
                "\u0000\u0000\u0000\uD5B5\u0000\u0000\uCAAA\uE1F9\u0000\uEAB1" + // 20980 - 20989
                "\u0000\u0000\u0000\uEAD6\uF1B0\u0000\u0000\u0000\uE2EE\u0000" + // 20990 - 20999
                "\u0000\u0000\u0000\uE5E7\u0000\u0000\u0000\uCAA3\uDDB2\u0000" + // 21000 - 21009
                "\u0000\u0000\u0000\uE6A9\u0000\uEFF3\uFDE9\u0000\uCFC1\u0000" + // 21010 - 21019
                "\uE0DF\uDEEC\u0000\u0000\uEDB8\u0000\u0000\u0000\uDBB6\u0000" + // 21020 - 21029
                "\u0000\uE6F0\u9AB6\u0000\u0000\u0000\u0000\u0000\uB4FD\uB4FE" + // 21030 - 21039
                "\u0000\uB5A1\uD7FC\u0000\uEDBB\u0000\u0000\uF6AB\u0000\u0000" + // 21040 - 21049
                "\uE0B5\u0000\u0000\u0000\u0000\u0000\u0000\uDEFA\u0000\uD7F8" + // 21050 - 21059
                "\uD5C4\u9CD4\u0000\u0000\u0000\u0000\u0000\uEDF4\uD4EB\u0000" + // 21060 - 21069
                "\uDEA2\u0000\u0000\u0000\uE5E6\u0000\uF3A7\u0000\u0000\uCDEA" + // 21070 - 21079
                "\u0000\uEBEE\u0000\u0000\uD8B4\u0000\u0000\u0000\u0000\u0000" + // 21080 - 21089
                "\u0000\uE9E4\u0000\u0000\uD7A5\u0000\u0000\u0000\u0000\uF7E8" + // 21090 - 21099
                "\u0000\uA8A2\u0000\u0000\u0000\u0000\u0000\u0000\uA1BF\uF8B3" + // 21100 - 21109
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uEDB6\uCEEF\u0000" + // 21110 - 21119
                "\u0000\uF2F3\u0000\u0000\u0000\u0000\uF4EF\u0000\u0000\u0000" + // 21120 - 21129
                "\uF6CE\uCCAE\u0000\uDADB\u0000\u0000\u0000\u0000\uCDC7\uDBB9" + // 21130 - 21139
                "\u0000\u9AF4\u0000\u0000\u0000\u0000\u0000\uB6F7\uB6F8\u0000" + // 21140 - 21149
                "\uB6F9\uEDF3\uDCD9\uE0CD\u0000\u0000\u0000\u0000\uF7DA\uE9A6" + // 21150 - 21159
                "\uCBF2\u0000\u0000\u0000\u0000\u0000\u0000\uC7A8\u0000\uC7A9" + // 21160 - 21169
                "\uDDAF\uDDB0\u0000\u9BEA\uCBB7\uE8D3\u0000\u0000\uDDF8\u0000" + // 21170 - 21179
                "\u9AE4\u0000\u0000\u0000\uE8CF\uE8D2\u0000\uCAC5\uCCEB\u0000" + // 21180 - 21189
                "\u0000\u0000\u0000\uE5A6\u0000\u0000\u0000\u0000\uC0EB\uC0EC" + // 21190 - 21199
                "\u0000\uC0ED\uD8E6\u9BBD\uF4B1\u0000\u0000\u0000\u0000\u0000" + // 21200 - 21209
                "\uB6F3\uB6F4\u0000\u0000\uD1B3\u0000\u0000\u0000\u0000\u0000" + // 21210 - 21219
                "\uEFED\uFDD8\u0000\u0000\u0000\u0000\uD2F6\u0000\u0000\uCFBB" + // 21220 - 21229
                "\u0000\u0000\u0000\uD3AD\uE8E1\uCEEC\u9DBC\u0000\u9AE8\uF9FD" + // 21230 - 21239
                "\u0000\uCADC\u0000\u0000\uE2B2\u0000\uD4BD\u0000\u9BE8\uD9CE" + // 21240 - 21249
                "\u0000\uF6B6\u0000\uCEC2\uD6C7\u0000\uE3B4\u0000\uF1AD\uF5C6" + // 21250 - 21259
                "\u0000\uE1A2\uE9C6\u0000\u0000\u0000\uF2C5\uDEBD\u0000\uF6A9" + // 21260 - 21269
                "\u0000\u0000\u0000\uDAA4\u0000\uDBD8\u0000\u0000\uCAA2\u0000" + // 21270 - 21279
                "\u0000\uD1CD\u0000\uA2AC\uA9F6\uA8AC\u0000\uA8F9\uA8F6\uA8FA" + // 21280 - 21289
                "\uA2AF\uE9FC\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD9EE" + // 21290 - 21299
                "\uD2B1\u0000\u0000\u0000\u9CF7\uCCE9\u0000\uD9C4\uE9A5\uD6D5" + // 21300 - 21309
                "\u0000\uCDC5\u9BBF\uEDBA\uD1BD\u0000\uF1A5\uE9CE\u0000\u0000" + // 21310 - 21319
                "\u0000\uF9BC\u0000\u0000\uDECF\u0000\u0000\u0000\u0000\u0000" + // 21320 - 21329
                "\u0000\uF8CC\u0000\uEAD9\uF9D7\u0000\u0000\u9CDB\u0000\u0000" + // 21330 - 21339
                "\u0000\u0000\uEEEC\u0000\u0000\uD3A3\uEEB7\uF6A8\uDDFD\u0000" + // 21340 - 21349
                "\u0000\u0000\u0000\u9DAA\u0000\uF8CF\u0000\u0000\u0000\u0000" + // 21350 - 21359
                "\uEAC8\uEEB8\uF1AC\uD7E8\uCBD8\u0000\u0000\u0000\uE9E2\u0000" + // 21360 - 21369
                "\u0000\uD4FD\u0000\u0000\uE0C8\u0000\u0000\u0000\u9FE0\uBDBF" + // 21370 - 21379
                "\uBDC0\u0000\uBDC1\uE0CC\uEBF9\u0000\u0000\u0000\u0000\u0000" + // 21380 - 21389
                "\u9AAE\u9CCF\u0000\uD6BE\u0000\u0000\u0000\uE2BA\u0000\uE3DF" + // 21390 - 21399
                "\u0000\uDEC3\u0000\uDEC4\uCAA1\u0000\u0000\uECF5\uE8EE\u0000" + // 21400 - 21409
                "\uCBA9\uF1AF\u0000\u0000\uEACE\u0000\uE8DF\u0000\u0000\u0000" + // 21410 - 21419
                "\u0000\uC2D5\u0000\u0000\u0000\uE7C5\u9DA2\u0000\uE0E9\u0000" + // 21420 - 21429
                "\uA1C6\uA1BE\uA9F7\uA9F8\uA2A5\u0000\uA2D2\u0000\uC8D5\u0000" + // 21430 - 21439
                "\u0000\u0000\u0000\u0000\u0000\uFCF5\u0000\u0000\u0000\uE0E6" + // 21440 - 21449
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF4D8\uD6B3\uDDAD\uD1BC" + // 21450 - 21459
                "\u0000\uE5CF\u0000\uCBB6\u0000\uDAB8\u0000\uDBE9\uFDCC\u0000" + // 21460 - 21469
                "\u0000\u0000\u0000\u0000\u0000\uD0A9\u0000\u0000\uEDAB\u0000" + // 21470 - 21479
                "\uE3B7\u0000\u0000\u0000\u0000\uC2CD\uC2CE\u0000\uC2CF\uDBEB" + // 21480 - 21489
                "\u0000\uDFFE\u0000\u0000\uD8E1\u0000\uF7F3\u9BDB\u0000\u0000" + // 21490 - 21499
                "\u0000\u0000\u0000\u0000\u0000\uEBC9\uCEB8\u0000\u0000\u0000" + // 21500 - 21509
                "\uD8D2\uF9D6\u0000\u0000\uE1C4\u0000\u0000\u0000\u0000\u0000" + // 21510 - 21519
                "\uE8B0\uF9FC\u0000\uCCC0\u0000\u0000\u0000\u0000\u0000\uB6D8" + // 21520 - 21529
                "\u0000\u0000\u0000\uA2C6\u0000\u0000\u0000\u0000\u0000\uF0B9" + // 21530 - 21539
                "\uE4FE\uE4C9\u0000\uE4E6\u0000\uF1EA\u0000\u0000\u0000\uCBEC" + // 21540 - 21549
                "\uCBC0\uF3C5\u0000\u0000\uD4C0\uD5BF\u0000\u0000\u0000\uA1D2" + // 21550 - 21559
                "\u0000\u0000\u0000\u0000\u0000\u9DF0\u9CC6\u0000\u0000\uF8DE" + // 21560 - 21569
                "\uF9AA\uCAF7\u0000\uEDB7\u0000\u0000\uEFE8\u0000\u0000\uE1BF" + // 21570 - 21579
                "\u0000\u0000\u0000\u9CED\uD0A1\u0000\u0000\u0000\uCEF7\u0000" + // 21580 - 21589
                "\u0000\uE0D8\u0000\uDCF5\uE0B9\u0000\u0000\u0000\uD4CE\u9CF4" + // 21590 - 21599
                "\uF4B5\uF0DB\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE7BD" + // 21600 - 21609
                "\uF8BA\uE8D0\u0000\u0000\uD8FB\u0000\u0000\uEAD5\uF4F3\uDAC9" + // 21610 - 21619
                "\u0000\uE6DE\u0000\u0000\u9CBB\u0000\uE4A7\uECD2\u0000\u0000" + // 21620 - 21629
                "\uF6B1\u0000\u9BDA\uCEFB\uF9E5\u0000\uE0CA\u0000\u0000\uF2FD" + // 21630 - 21639
                "\uD3B0\u0000\uFAFB\u0000\u0000\uFABD\uCCC8\uEFCD\uD5D5\u0000" + // 21640 - 21649
                "\uA1A7\u0000\uA8A3\u0000\u0081\u0000\u0000\u0000\uD1A8\u0000" + // 21650 - 21659
                "\u0000\u0000\u0000\uC5E1\u0000\u0000\u0000\uD8C8\u0000\u0000" + // 21660 - 21669
                "\uEEC1\u0000\uC8CB\u0000\u0000\u0000\u0000\u0000\u0000\uFBE0" + // 21670 - 21679
                "\uF2E5\u0000\u0000\uC6FE\u0000\u0000\u0000\u0000\u0000\u0000" + // 21680 - 21689
                "\uEFFB\u0000\u0000\uFAF9\uD7C6\u0000\uD1BB\uF7AA\u0000\uEDCA" + // 21690 - 21699
                "\uD7D3\uD8FA\uD6E0\u0000\uF1C6\u0000\u0000\u0000\u0000\u0000" + // 21700 - 21709
                "\uB6CD\u0000\u0000\u0000\uA1D6\u0000\u0000\u0000\u0000\u0000" + // 21710 - 21719
                "\uFCA8\u0000\u0000\uECE6\uEBD6\u0000\uECDF\u0000\u0000\u0000" + // 21720 - 21729
                "\uDFFC\u0000\uD0E6\u0000\u0000\uDEC1\u0000\u0000\uE4AC\u0000" + // 21730 - 21739
                "\u0078\u0079\u007A\u007B\u007C\u007D\u007E\u007F\uCCBF\u0000" + // 21740 - 21749
                "\u0000\u0000\u0000\u0000\u0000\u0000\uFDC8\uE1AC\u0000\u0000" + // 21750 - 21759
                "\uE3EB\u0000\uEEC7\u0000\u0000\uE2DB\u0000\u0000\u0000\uDFDE" + // 21760 - 21769
                "\u0000\uE0C7\uE1C8\uDBB7\uDFE3\u0000\u0000\u0000\u0000\u0000" + // 21770 - 21779
                "\u9EF6\u0000\u0000\u0000\uA2A1\u0000\uA2A2\u0000\u0000\u0000" + // 21780 - 21789
                "\uD7FA\u0000\u0000\u0000\uFBC8\uCEDC\uF2B5\uD0E4\uDDD1\u0000" + // 21790 - 21799
                "\u0000\u0000\u0000\uA8FB\uA8FC\uA8FD\uA8FE\u0000\uEAA7\uE9F6" + // 21800 - 21809
                "\uFBBB\u0000\uE7E9\uEFCC\u0000\u0000\uD9D8\u0000\u0000\u0000" + // 21810 - 21819
                "\u0000\u0000\u0000\uD8D5\u0000\u0000\uD8D9\u0000\uF4A3\u0000" + // 21820 - 21829
                "\u0000\uF4DD\u0000\u0070\u0071\u0072\u0073\u0074\u0075\u0076" + // 21830 - 21839
                "\u0077\uD2EF\u0000\u0000\u0000\uE2ED\u0000\u0000\uDEE9\uFCBC" + // 21840 - 21849
                "\u0000\uDAA2\uDAA3\u0000\uD2A1\u0000\u0000\uF2D4\u0000\uD1B0" + // 21850 - 21859
                "\u0000\uCCE0\u0000\uDBFD\uD1BA\u0000\uF1C4\u0000\uE5B3\uFBF5" + // 21860 - 21869
                "\uE9E1\uFDE0\uCBB3\u0000\u0000\u0000\u0000\u0000\u0000\uD5DD" + // 21870 - 21879
                "\uEFC4\u0000\u0000\u0000\u0000\u0000\u0000\uE1D8\uD6EB\u0000" + // 21880 - 21889
                "\u0000\u0000\uF4D9\u9CCE\u0000\u0000\uD2C5\uFBD1\uE7C0\uEBA5" + // 21890 - 21899
                "\u0000\uDFFA\uE3A2\u9DC6\u0000\u0000\u0000\u0000\u0000\u0000" + // 21900 - 21909
                "\uF0EA\uE1C6\u0000\u0000\u0000\uD4BF\u0000\u9BE9\u0000\uE5F8" + // 21910 - 21919
                "\u0000\u0000\uDEC0\uECA3\u0000\uE9CD\u0000\u0068\u0069\u006A" + // 21920 - 21929
                "\u006B\u006C\u006D\u006E\u006F\uEFBD\uFCD6\u0000\u0000\uDBF4" + // 21930 - 21939
                "\u0000\uEFAA\uF8B9\uEEC6\u0000\u0000\u0000\u0000\u0000\u0000" + // 21940 - 21949
                "\u0000\uF4F2\uD0B5\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 21950 - 21959
                "\uE9C0\uCECC\uF5E8\uF7D5\u0000\uD3CD\u0000\uF3FE\u0000\uE3AB" + // 21960 - 21969
                "\uEBE0\u0000\u0000\u0000\uCEFA\uCBF7\uE5A5\uD8A2\u0000\u0000" + // 21970 - 21979
                "\u0000\u0000\u9BF6\uDDAC\u0000\uFCAF\uD3A1\u0000\uF1AB\u0000" + // 21980 - 21989
                "\u0000\u0000\u0000\uC0B8\uC0B9\u0000\u0000\uE3F7\u0000\u0000" + // 21990 - 21999
                "\u0000\u0000\u0000\uECA8\u9AD2\u0000\u9DB5\u0000\u0000\u0000" + // 22000 - 22009
                "\u0000\uFBEE\uEDF1\u0000\u0000\uF1E2\u0000\uD4DB\u0000\u0000" + // 22010 - 22019
                "\uFBA8\uD0A8\u0000\u0000\uDAEC\u0000\u0000\uE7B8\u0000\u0000" + // 22020 - 22029
                "\u0000\u0000\u0000\u0000\u9AC1\u9BFB\u0000\u9BD8\u0000\uCDFA" + // 22030 - 22039
                "\u0000\u0000\u0000\u0000\u0000\uF8FD\u0000\u0000\uF8FC\u9DB4" + // 22040 - 22049
                "\u0000\uEFBC\uD8A1\u0000\u0000\u0000\u0000\uC8DE\uC8DF\u0000" + // 22050 - 22059
                "\u0000\uCDF5\u0000\u0000\u0000\uFDB0\uD5A8\u0000\uCEF8\uDCB0" + // 22060 - 22069
                "\u0000\u0000\u0000\u0000\uE3AA\u0000\u0060\u0061\u0062\u0063" + // 22070 - 22079
                "\u0064\u0065\u0066\u0067\uCFD7\u0000\u0000\u0000\u0000\u0000" + // 22080 - 22089
                "\u0000\uCFDF\uE9A1\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 22090 - 22099
                "\uDFB6\uE5CD\u0000\u0000\u0000\uFAEB\u0000\uCFBC\u0000\uEDDB" + // 22100 - 22109
                "\uDFB2\uDFBE\uF9BB\u0000\uDCF4\u0000\u0000\uF4B8\uF7BC\uDCFD" + // 22110 - 22119
                "\u0000\uE8EC\uE4E7\u0000\u0058\u0059\u005A\u005B\\\u005D" + // 22120 - 22129 // Modified to map U-005C to 0x5C
                "\u005E\u005F\uCDDA\u0000\u0000\u0000\u0000\u0000\uD9CF\u0000" + // 22130 - 22139
                "\uECE9\uEFCB\u0000\uF6D2\u0000\u0000\u0000\uD8B2\uF0D6\u0000" + // 22140 - 22149
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE4F3\uCAD9\u0000\u0000" + // 22150 - 22159
                "\uEFEF\u0000\uF5AA\u0000\u0000\uE8CC\u0000\u0000\u0000\uDEB7" + // 22160 - 22169
                "\u0000\u0000\uCBC9\u0000\u9BAF\uE6D1\uF0CC\u0000\u0000\uDAC5" + // 22170 - 22179
                "\u0000\uD8EC\u0000\u0000\u0000\u0000\uC5DB\uC5DC\u0000\uC5DD" + // 22180 - 22189
                "\uFDFC\u0000\u0000\u0000\u0000\uE1AA\u0000\u0000\uE8AC\u0000" + // 22190 - 22199
                "\uE8DD\u0000\u0000\uEFE9\u0000\uA2E4\u0000\u0000\uA7E4\uA7EE" + // 22200 - 22209
                "\uA7E9\u0000\u0000\uF9C9\u0000\uE4E2\u0000\uFBBD\u0000\u0000" + // 22210 - 22219
                "\uECEC\uFBBE\uDFEB\u0000\uE1F8\u0000\u0000\uE2D4\uD2FD\u0000" + // 22220 - 22229
                "\uE5A8\u0000\u0000\u0000\u9DCF\u0000\u0000\u0000\u0000\uA1A4" + // 22230 - 22239
                "\u0000\u0000\u0000\u0000\uC0AB\uC0AC\u0000\uC0AD\uDDFA\u0000" + // 22240 - 22249
                "\u0000\u0000\u0000\u0000\u0000\uF0D5\uE2B3\uDEE7\u0000\u0000" + // 22250 - 22259
                "\u0000\u0000\u0000\u0000\uB7D8\u0000\u0000\uEEC3\u0000\u0000" + // 22260 - 22269
                "\u0000\u0000\u0000\u0000\uCEF6\u0000\uFAD0\uF8F9\u0000\u0000" + // 22270 - 22279
                "\u0000\u0000\uF0AE\u0000\u0000\u9CA8\u0000\uFDB8\uE3E8\u0000" + // 22280 - 22289
                "\uD4A7\uE8FC\uDEE6\u0000\u0000\u0000\u0000\uDFD4\u0000\u0000" + // 22290 - 22299
                "\uE7A7\u0000\uE6D7\u0000\u0000\u0000\u0000\uC6CA\uC6CB\u0000" + // 22300 - 22309
                "\uC6CC\uE9DE\u0000\u0000\u0000\u0000\u0000\uF0D3\uF2B4\uD1B7" + // 22310 - 22319
                "\uF2B3\u0000\u0000\u0000\u0000\u0000\u0000\uB0EB\u0000\uB0EC" + // 22320 - 22329
                "\uDEE5\uD1B5\u0000\u0000\u0000\u0000\u0000\uD1B6\uD8A8\u0000" + // 22330 - 22339
                "\u0000\u0000\uCCE4\u0000\u0000\uD1B4\uDAF1\u0000\u0000\u0000" + // 22340 - 22349
                "\u0000\u0000\u0000\u0000\uE6A2\uCAF9\u0000\u0000\uD4DA\u0000" + // 22350 - 22359
                "\u0000\u0000\u0000\uC7F0\uC7F1\u0000\uC7F2\u9CDA\u0000\u0000" + // 22360 - 22369
                "\uF4E2\u0000\u0000\u0000\u0000\uA0F7\u0000\u0000\u0000\u9FF1" + // 22370 - 22379
                "\uBEAD\u0000\u0000\u0000\u9FF2\u9FF3\u0000\u0000\u0000\uBECE" + // 22380 - 22389
                "\uBECF\uBED0\u0000\uBED1\uF3B7\u0000\u0000\u0000\u0000\u0000" + // 22390 - 22399
                "\u0000\u0000\uDBFC\uD9C2\u0000\uF0D2\u0000\uE4D1\u0000\u0000" + // 22400 - 22409
                "\u0000\u9FE7\u0000\uBDE0\u0000\u0000\uE9FB\uEAA8\u0000\u0000" + // 22410 - 22419
                "\u0000\u0000\uFDB7\uD8F9\u0000\u0000\u0000\u0000\u9CF5\u0000" + // 22420 - 22429
                "\u0000\uE6D5\u0000\u0000\uE9F2\u0000\uDFB0\u0000\uA7EA\u0000" + // 22430 - 22439
                "\u0000\uA7EB\u0000\u0000\uA7DF\u0000\u0050\u0051\u0052\u0053" + // 22440 - 22449
                "\u0054\u0055\u0056\u0057\uF7AF\uDAB6\u0000\uCAD7\u0000\u0000" + // 22450 - 22459
                "\u0000\u0000\uC7CB\uC7CC\u0000\uC7CD\uDFD3\u0000\u0000\u0000" + // 22460 - 22469
                "\uDAF0\u0000\uE2EA\u0000\uA7BC\uA7ED\uA7B5\u0000\u0000\u0000" + // 22470 - 22479
                "\u0000\uA7B9\uE7C3\u0000\uECCC\u0000\u0000\u0000\u0000\u0000" + // 22480 - 22489
                "\uB5E5\uB5E6\u0000\u0000\uD9ED\u0000\u0000\u0000\u0000\uF5A5" + // 22490 - 22499
                "\u0000\uA7DA\uA7DB\uA2E3\uA7EC\uA7A6\uA7E0\uA7EF\uA2E1\uCDC1" + // 22500 - 22509
                "\u0000\u0000\uFBD3\u0000\u0000\u0000\u0000\uC7C7\uC7C8\u0000" + // 22510 - 22519
                "\u0000\uDBCC\uDDCD\u0000\u0000\u0000\uD4C8\u0000\uA7C7\uA7C8" + // 22520 - 22529
                "\uA7CE\uA7CF\uA7D0\uA7D1\uA7D2\uA7D3\uCDA4\u0000\u0000\uD4F4" + // 22530 - 22539
                "\uDBA1\uDBDC\uDBDD\u0000\uA7BF\uA7C0\uA7C1\uA7C2\uA7C3\uA7C4" + // 22540 - 22549
                "\uA7C5\uA7C6\uE8B9\u0000\uEFA6\u0000\u0000\u0000\u0000\u0000" + // 22550 - 22559
                "\u9EED\uB5DD\u0000\uB5DE\u9AF3\u0000\u0000\u0000\u0000\u0000" + // 22560 - 22569
                "\u0000\u0000\uD9A7\uF4B0\uF3EA\uDAEE\u0000\uD7BB\u0000\uE2B1" + // 22570 - 22579
                "\u9DF4\uE5DC\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCDD9" + // 22580 - 22589
                "\uD3C3\u0000\uD8A6\u9BB3\uF6C1\u0000\u0000\u0000\uB6D5\uB6D6" + // 22590 - 22599
                "\u0000\u0000\u0000\uB8DD\uB8DE\uB8DF\u0000\u0000\uF8B2\u0000" + // 22600 - 22609
                "\u0000\u0000\uDCEB\u0000\u0000\uFBC7\uD5C8\u0000\uD7DF\u0000" + // 22610 - 22619
                "\uDDA9\u0000\uA7BE\uA7E5\uA7E6\uA7E7\uA7E8\uA7E1\uA7E2\uA7E3" + // 22620 - 22629
                "\uD4E3\uCCE2\u0000\uF7D4\u0000\u0000\u0000\u0000\uC7B6\u0000" + // 22630 - 22639
                "\u0000\u0000\uB6BB\uB6BC\uB6BD\u0000\u0000\uCCDE\u0000\u0000" + // 22640 - 22649
                "\u0000\u0000\u0000\u0000\uDCE0\u0000\u0000\uEFBA\uF1DD\u0000" + // 22650 - 22659
                "\uDEB3\u0000\u0000\u0000\u9AE1\u0000\u0000\uF4C7\u0000\uA7B2" + // 22660 - 22669
                "\uA7B3\uA7B4\uA7A7\uA7A8\uA7A9\uA7AA\uA7BD\uD3B8\uF2D6\u0000" + // 22670 - 22679
                "\u0000\uD4D9\uEEC5\uF2F0\u0000\uA7A5\uA7AB\uA7AC\uA7AD\uA7AE" + // 22680 - 22689
                "\uA7AF\uA7B0\uA7B1\uD1B1\u0000\uCBB1\u0000\u0000\u0000\u0000" + // 22690 - 22699
                "\uD1B2\uECB6\u0000\u0000\u0000\u0000\uFBFE\uD3D7\u0000\uA7D4" + // 22700 - 22709
                "\uA7D5\uA7D6\uA7D7\uA7D8\uA7A1\uA7A2\uA7A3\uEFA4\u0000\uEFEB" + // 22710 - 22719
                "\u0000\u0000\u0000\u0000\u0000\uB5D6\u0000\u0000\u0000\u9EDD" + // 22720 - 22729
                "\uB4B3\u0000\u0000\u0000\uB4E2\uB4E3\uB4E4\u0000\uB4E5\uEFA3" + // 22730 - 22739
                "\uEBA6\uCBA3\uE3E9\u0000\u0000\u0000\uD1FB\uE9C4\u0000\u0000" + // 22740 - 22749
                "\uDCCB\uE9C5\u0000\u0000\u0000\uB3D6\uB3D7\uB3D8\u0000\u0000" + // 22750 - 22759
                "\uE5C8\u0000\u0000\u0000\uFBA4\uD4B9\u0000\uA7BA\uA7BB\uA7DC" + // 22760 - 22769
                "\uA7DD\uA7DE\uA7B6\uA7B7\uA7B8\uCAF6\u0000\uE4A4\uF4D6\u0000" + // 22770 - 22779
                "\u0000\u0000\uDFE6\uFBD2\u0000\uF8F8\uF7FB\u0000\u0000\uE8BF" + // 22780 - 22789
                "\u0000\uA7C9\uA7CA\uA7CB\uA7CC\uA7CD\u0000\u0000\u0000\uFAAE" + // 22790 - 22799
                "\u0000\u0000\u0000\uD6E9\uCEB6\u0000\uF3C0\u0000\uCDFE\u0000" + // 22800 - 22809
                "\u0000\u0000\u9EB7\uB1C9\uB1CA\u0000\u0000\uFBCD\u0000\uD5BD" + // 22810 - 22819
                "\uF1DF\u0000\u0000\uF6FB\uFCBB\u0000\uE2B0\u0000\u0000\uE6A5" + // 22820 - 22829
                "\u0000\u0000\uD3C2\u0000\u0000\u0000\u0000\uD3B6\u0000\uA8C9" + // 22830 - 22839
                "\uA8CA\uA8CB\uA8CC\u0000\u0000\u0000\uA2DE\uF3BF\u0000\uF0D1" + // 22840 - 22849
                "\u0000\u0000\u0000\u0000\u0000\uB5CD\u0000\u0000\u0000\uB1BF" + // 22850 - 22859
                "\uB1C0\uB1C1\u0000\uB1C2\uD7F3\u0000\u0000\u0000\uFCD4\u0000" + // 22860 - 22869
                "\uDAD7\uCCDF\uF2D3\uFBA9\uD8A5\u0000\u0000\u0000\u0000\uD5CB"
                ;

        public Encoder(Charset cs) {
            super(cs, index2a);
        }
    }
}
