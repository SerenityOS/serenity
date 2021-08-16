/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import sun.nio.cs.*;
import sun.nio.cs.ext.*;


public class IBM970_OLD
    extends Charset
    implements HistoricallyNamedCharset
{

    private final static IBM970 nioCoder = new IBM970();

    public IBM970_OLD() {
        super("x-IBM970-Old", null);
    }

    public String historicalName() {
        return "Cp970";
    }

    public boolean contains(Charset cs) {
        return (cs instanceof IBM970);
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    public String getDecoderSingleByteMappings() {
        return Decoder.byteToCharTable;
    }

    public String getDecoderMappingTableG1() {
        return Decoder.mappingTableG1;
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

    protected static class Decoder extends SimpleEUCDecoder {

        public Decoder(Charset cs) {
            super(cs);
            super.byteToCharTable = byteToCharTable;
            super.mappingTableG1 = mappingTableG1;
        }

        private final static String byteToCharTable;
        private final static String mappingTableG1;
        static {
            byteToCharTable =
                "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007" +
                "\u0008\u0009\n\u000B\u000C\r\u000E\u000F" +
                "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
                "\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F" +
                "\u0020\u0021\"\u0023\u0024\u0025\u0026\u0027" +
                "\u0028\u0029\u002A\u002B\u002C\u002D\u002E\u002F" +
                "\u0030\u0031\u0032\u0033\u0034\u0035\u0036\u0037" +
                "\u0038\u0039\u003A\u003B\u003C\u003D\u003E\u003F" +
                "\u0040\u0041\u0042\u0043\u0044\u0045\u0046\u0047" +
                "\u0048\u0049\u004A\u004B\u004C\u004D\u004E\u004F" +
                "\u0050\u0051\u0052\u0053\u0054\u0055\u0056\u0057" +
                "\u0058\u0059\u005A\u005B\\\u005D\u005E\u005F" +
                "\u0060\u0061\u0062\u0063\u0064\u0065\u0066\u0067" +
                "\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F" +
                "\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077" +
                "\u0078\u0079\u007A\u007B\u007C\u007D\u007E\u007F" +
                "\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087" +
                "\u0088\u0089\u008A\u008B\u008C\u008D\uFFFD\uFFFD" +
                "\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097" +
                "\u0098\u0099\u009A\u009B\u009C\u009D\u009E\u009F"
                ;
            mappingTableG1 =
                "\u3000\u3001\u3002\u30FB\u2025\u2026\u00A8\u3003" +
                "\u2010\u2014\u2225\uFF3C\u301C\u2018\u2019\u201C" +
                "\u201D\u3014\u3015\u3008\u3009\u300A\u300B\u300C" +
                "\u300D\u300E\u300F\u3010\u3011\u00B1\u00D7\u00F7" +
                "\u2260\u2264\u2265\u221E\u2234\u00B0\u2032\u2033" +
                "\u2103\u212B\uFFE0\uFFE1\uFFE5\u2642\u2640\u2220" +
                "\u22A5\u2312\u2202\u2207\u2261\u2252\u00A7\u203B" +
                "\u2606\u2605\u25CB\u25CF\u25CE\u25C7\u25C6\u25A1" +
                "\u25A0\u25B3\u25B2\u25BD\u25BC\u2192\u2190\u2191" +
                "\u2193\u2194\u3013\u226A\u226B\u221A\u223D\u221D" +
                "\u2235\u222B\u222C\u2208\u220B\u2286\u2287\u2282" +
                "\u2283\u222A\u2229\u2227\u2228\uFFE2\u21D2\u21D4" +
                "\u2200\u2203\u00B4\u02DC\u02C7\u02D8\u02DD\u02DA" +
                "\u02D9\u00B8\u02DB\u00A1\u00BF\u02D0\u222E\u2211" +
                "\u220F\u00A4\u2109\u2030\u25C1\u25C0\u25B7\u25B6" +
                "\u2664\u2660\u2661\u2665\u2667\u2663\u2299\u25C8" +
                "\u25A3\u25D0\u25D1\u2592\u25A4\u25A5\u25A8\u25A7" +
                "\u25A6\u25A9\u2668\u260F\u260E\u261C\u261E\u00B6" +
                "\u2020\u2021\u2195\u2197\u2199\u2196\u2198\u266D" +
                "\u2669\u266A\u266C\u327F\u321C\u2116\u33C7\u2122" +
                "\u33C2\u33D8\u2121\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFF01\uFF02\uFF03\uFF04" +
                "\uFF05\uFF06\uFF07\uFF08\uFF09\uFF0A\uFF0B\uFF0C" +
                "\uFF0D\uFF0E\uFF0F\uFF10\uFF11\uFF12\uFF13\uFF14" +
                "\uFF15\uFF16\uFF17\uFF18\uFF19\uFF1A\uFF1B\uFF1C" +
                "\uFF1D\uFF1E\uFF1F\uFF20\uFF21\uFF22\uFF23\uFF24" +
                "\uFF25\uFF26\uFF27\uFF28\uFF29\uFF2A\uFF2B\uFF2C" +
                "\uFF2D\uFF2E\uFF2F\uFF30\uFF31\uFF32\uFF33\uFF34" +
                "\uFF35\uFF36\uFF37\uFF38\uFF39\uFF3A\uFF3B\uFFE6" +
                "\uFF3D\uFF3E\uFF3F\uFF40\uFF41\uFF42\uFF43\uFF44" +
                "\uFF45\uFF46\uFF47\uFF48\uFF49\uFF4A\uFF4B\uFF4C" +
                "\uFF4D\uFF4E\uFF4F\uFF50\uFF51\uFF52\uFF53\uFF54" +
                "\uFF55\uFF56\uFF57\uFF58\uFF59\uFF5A\uFF5B\uFF5C" +
                "\uFF5D\uFFE3\u3131\u3132\u3133\u3134\u3135\u3136" +
                "\u3137\u3138\u3139\u313A\u313B\u313C\u313D\u313E" +
                "\u313F\u3140\u3141\u3142\u3143\u3144\u3145\u3146" +
                "\u3147\u3148\u3149\u314A\u314B\u314C\u314D\u314E" +
                "\u314F\u3150\u3151\u3152\u3153\u3154\u3155\u3156" +
                "\u3157\u3158\u3159\u315A\u315B\u315C\u315D\u315E" +
                "\u315F\u3160\u3161\u3162\u3163\u3164\u3165\u3166" +
                "\u3167\u3168\u3169\u316A\u316B\u316C\u316D\u316E" +
                "\u316F\u3170\u3171\u3172\u3173\u3174\u3175\u3176" +
                "\u3177\u3178\u3179\u317A\u317B\u317C\u317D\u317E" +
                "\u317F\u3180\u3181\u3182\u3183\u3184\u3185\u3186" +
                "\u3187\u3188\u3189\u318A\u318B\u318C\u318D\u318E" +
                "\u2170\u2171\u2172\u2173\u2174\u2175\u2176\u2177" +
                "\u2178\u2179\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\u2160" +
                "\u2161\u2162\u2163\u2164\u2165\u2166\u2167\u2168" +
                "\u2169\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\u0391\u0392\u0393\u0394\u0395\u0396\u0397\u0398" +
                "\u0399\u039A\u039B\u039C\u039D\u039E\u039F\u03A0" +
                "\u03A1\u03A3\u03A4\u03A5\u03A6\u03A7\u03A8\u03A9" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\u03B1\u03B2\u03B3\u03B4\u03B5\u03B6\u03B7\u03B8" +
                "\u03B9\u03BA\u03BB\u03BC\u03BD\u03BE\u03BF\u03C0" +
                "\u03C1\u03C3\u03C4\u03C5\u03C6\u03C7\u03C8\u03C9" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\u2500\u2502" +
                "\u250C\u2510\u2518\u2514\u251C\u252C\u2524\u2534" +
                "\u253C\u2501\u2503\u250F\u2513\u251B\u2517\u2523" +
                "\u2533\u252B\u253B\u254B\u2520\u252F\u2528\u2537" +
                "\u253F\u251D\u2530\u2525\u2538\u2542\u2512\u2511" +
                "\u251A\u2519\u2516\u2515\u250E\u250D\u251E\u251F" +
                "\u2521\u2522\u2526\u2527\u2529\u252A\u252D\u252E" +
                "\u2531\u2532\u2535\u2536\u2539\u253A\u253D\u253E" +
                "\u2540\u2541\u2543\u2544\u2545\u2546\u2547\u2548" +
                "\u2549\u254A\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\u3395\u3396\u3397\u2113" +
                "\u3398\u33C4\u33A3\u33A4\u33A5\u33A6\u3399\u339A" +
                "\u339B\u339C\u339D\u339E\u339F\u33A0\u33A1\u33A2" +
                "\u33CA\u338D\u338E\u338F\u33CF\u3388\u3389\u33C8" +
                "\u33A7\u33A8\u33B0\u33B1\u33B2\u33B3\u33B4\u33B5" +
                "\u33B6\u33B7\u33B8\u33B9\u3380\u3381\u3382\u3383" +
                "\u3384\u33BA\u33BB\u33BC\u33BD\u33BE\u33BF\u3390" +
                "\u3391\u3392\u3393\u3394\u2126\u33C0\u33C1\u338A" +
                "\u338B\u338C\u33D6\u33C5\u33AD\u33AE\u33AF\u33DB" +
                "\u33A9\u33AA\u33AB\u33AC\u33DD\u33D0\u33D3\u33C3" +
                "\u33C9\u33DC\u33C6\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\u00C6\u00D0\u00AA\u0126\uFFFD\u0132" +
                "\uFFFD\u013F\u0141\u00D8\u0152\u00BA\u00DE\u0166" +
                "\u014A\uFFFD\u3260\u3261\u3262\u3263\u3264\u3265" +
                "\u3266\u3267\u3268\u3269\u326A\u326B\u326C\u326D" +
                "\u326E\u326F\u3270\u3271\u3272\u3273\u3274\u3275" +
                "\u3276\u3277\u3278\u3279\u327A\u327B\u24D0\u24D1" +
                "\u24D2\u24D3\u24D4\u24D5\u24D6\u24D7\u24D8\u24D9" +
                "\u24DA\u24DB\u24DC\u24DD\u24DE\u24DF\u24E0\u24E1" +
                "\u24E2\u24E3\u24E4\u24E5\u24E6\u24E7\u24E8\u24E9" +
                "\u2460\u2461\u2462\u2463\u2464\u2465\u2466\u2467" +
                "\u2468\u2469\u246A\u246B\u246C\u246D\u246E\u00BD" +
                "\u2153\u2154\u00BC\u00BE\u215B\u215C\u215D\u215E" +
                "\u00E6\u0111\u00F0\u0127\u0131\u0133\u0138\u0140" +
                "\u0142\u00F8\u0153\u00DF\u00FE\u0167\u014B\u0149" +
                "\u3200\u3201\u3202\u3203\u3204\u3205\u3206\u3207" +
                "\u3208\u3209\u320A\u320B\u320C\u320D\u320E\u320F" +
                "\u3210\u3211\u3212\u3213\u3214\u3215\u3216\u3217" +
                "\u3218\u3219\u321A\u321B\u249C\u249D\u249E\u249F" +
                "\u24A0\u24A1\u24A2\u24A3\u24A4\u24A5\u24A6\u24A7" +
                "\u24A8\u24A9\u24AA\u24AB\u24AC\u24AD\u24AE\u24AF" +
                "\u24B0\u24B1\u24B2\u24B3\u24B4\u24B5\u2474\u2475" +
                "\u2476\u2477\u2478\u2479\u247A\u247B\u247C\u247D" +
                "\u247E\u247F\u2480\u2481\u2482\u00B9\u00B2\u00B3" +
                "\u2074\u207F\u2081\u2082\u2083\u2084\u3041\u3042" +
                "\u3043\u3044\u3045\u3046\u3047\u3048\u3049\u304A" +
                "\u304B\u304C\u304D\u304E\u304F\u3050\u3051\u3052" +
                "\u3053\u3054\u3055\u3056\u3057\u3058\u3059\u305A" +
                "\u305B\u305C\u305D\u305E\u305F\u3060\u3061\u3062" +
                "\u3063\u3064\u3065\u3066\u3067\u3068\u3069\u306A" +
                "\u306B\u306C\u306D\u306E\u306F\u3070\u3071\u3072" +
                "\u3073\u3074\u3075\u3076\u3077\u3078\u3079\u307A" +
                "\u307B\u307C\u307D\u307E\u307F\u3080\u3081\u3082" +
                "\u3083\u3084\u3085\u3086\u3087\u3088\u3089\u308A" +
                "\u308B\u308C\u308D\u308E\u308F\u3090\u3091\u3092" +
                "\u3093\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\u30A1\u30A2\u30A3\u30A4" +
                "\u30A5\u30A6\u30A7\u30A8\u30A9\u30AA\u30AB\u30AC" +
                "\u30AD\u30AE\u30AF\u30B0\u30B1\u30B2\u30B3\u30B4" +
                "\u30B5\u30B6\u30B7\u30B8\u30B9\u30BA\u30BB\u30BC" +
                "\u30BD\u30BE\u30BF\u30C0\u30C1\u30C2\u30C3\u30C4" +
                "\u30C5\u30C6\u30C7\u30C8\u30C9\u30CA\u30CB\u30CC" +
                "\u30CD\u30CE\u30CF\u30D0\u30D1\u30D2\u30D3\u30D4" +
                "\u30D5\u30D6\u30D7\u30D8\u30D9\u30DA\u30DB\u30DC" +
                "\u30DD\u30DE\u30DF\u30E0\u30E1\u30E2\u30E3\u30E4" +
                "\u30E5\u30E6\u30E7\u30E8\u30E9\u30EA\u30EB\u30EC" +
                "\u30ED\u30EE\u30EF\u30F0\u30F1\u30F2\u30F3\u30F4" +
                "\u30F5\u30F6\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\u0410\u0411\u0412\u0413\u0414\u0415" +
                "\u0401\u0416\u0417\u0418\u0419\u041A\u041B\u041C" +
                "\u041D\u041E\u041F\u0420\u0421\u0422\u0423\u0424" +
                "\u0425\u0426\u0427\u0428\u0429\u042A\u042B\u042C" +
                "\u042D\u042E\u042F\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\u0430\u0431\u0432\u0433\u0434\u0435" +
                "\u0451\u0436\u0437\u0438\u0439\u043A\u043B\u043C" +
                "\u043D\u043E\u043F\u0440\u0441\u0442\u0443\u0444" +
                "\u0445\u0446\u0447\u0448\u0449\u044A\u044B\u044C" +
                "\u044D\u044E\u044F\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" +
                "\uFFFD\uFFFD\uAC00\uAC01\uAC04\uAC07\uAC08\uAC09" +
                "\uAC0A\uAC10\uAC11\uAC12\uAC13\uAC14\uAC15\uAC16" +
                "\uAC17\uAC19\uAC1A\uAC1B\uAC1C\uAC1D\uAC20\uAC24" +
                "\uAC2C\uAC2D\uAC2F\uAC30\uAC31\uAC38\uAC39\uAC3C" +
                "\uAC40\uAC4B\uAC4D\uAC54\uAC58\uAC5C\uAC70\uAC71" +
                "\uAC74\uAC77\uAC78\uAC7A\uAC80\uAC81\uAC83\uAC84" +
                "\uAC85\uAC86\uAC89\uAC8A\uAC8B\uAC8C\uAC90\uAC94" +
                "\uAC9C\uAC9D\uAC9F\uACA0\uACA1\uACA8\uACA9\uACAA" +
                "\uACAC\uACAF\uACB0\uACB8\uACB9\uACBB\uACBC\uACBD" +
                "\uACC1\uACC4\uACC8\uACCC\uACD5\uACD7\uACE0\uACE1" +
                "\uACE4\uACE7\uACE8\uACEA\uACEC\uACEF\uACF0\uACF1" +
                "\uACF3\uACF5\uACF6\uACFC\uACFD\uAD00\uAD04\uAD06" +
                "\uAD0C\uAD0D\uAD0F\uAD11\uAD18\uAD1C\uAD20\uAD29" +
                "\uAD2C\uAD2D\uAD34\uAD35\uAD38\uAD3C\uAD44\uAD45" +
                "\uAD47\uAD49\uAD50\uAD54\uAD58\uAD61\uAD63\uAD6C" +
                "\uAD6D\uAD70\uAD73\uAD74\uAD75\uAD76\uAD7B\uAD7C" +
                "\uAD7D\uAD7F\uAD81\uAD82\uAD88\uAD89\uAD8C\uAD90" +
                "\uAD9C\uAD9D\uADA4\uADB7\uADC0\uADC1\uADC4\uADC8" +
                "\uADD0\uADD1\uADD3\uADDC\uADE0\uADE4\uADF8\uADF9" +
                "\uADFC\uADFF\uAE00\uAE01\uAE08\uAE09\uAE0B\uAE0D" +
                "\uAE14\uAE30\uAE31\uAE34\uAE37\uAE38\uAE3A\uAE40" +
                "\uAE41\uAE43\uAE45\uAE46\uAE4A\uAE4C\uAE4D\uAE4E" +
                "\uAE50\uAE54\uAE56\uAE5C\uAE5D\uAE5F\uAE60\uAE61" +
                "\uAE65\uAE68\uAE69\uAE6C\uAE70\uAE78\uAE79\uAE7B" +
                "\uAE7C\uAE7D\uAE84\uAE85\uAE8C\uAEBC\uAEBD\uAEBE" +
                "\uAEC0\uAEC4\uAECC\uAECD\uAECF\uAED0\uAED1\uAED8" +
                "\uAED9\uAEDC\uAEE8\uAEEB\uAEED\uAEF4\uAEF8\uAEFC" +
                "\uAF07\uAF08\uAF0D\uAF10\uAF2C\uAF2D\uAF30\uAF32" +
                "\uAF34\uAF3C\uAF3D\uAF3F\uAF41\uAF42\uAF43\uAF48" +
                "\uAF49\uAF50\uAF5C\uAF5D\uAF64\uAF65\uAF79\uAF80" +
                "\uAF84\uAF88\uAF90\uAF91\uAF95\uAF9C\uAFB8\uAFB9" +
                "\uAFBC\uAFC0\uAFC7\uAFC8\uAFC9\uAFCB\uAFCD\uAFCE" +
                "\uAFD4\uAFDC\uAFE8\uAFE9\uAFF0\uAFF1\uAFF4\uAFF8" +
                "\uB000\uB001\uB004\uB00C\uB010\uB014\uB01C\uB01D" +
                "\uB028\uB044\uB045\uB048\uB04A\uB04C\uB04E\uB053" +
                "\uB054\uB055\uB057\uB059\uB05D\uB07C\uB07D\uB080" +
                "\uB084\uB08C\uB08D\uB08F\uB091\uB098\uB099\uB09A" +
                "\uB09C\uB09F\uB0A0\uB0A1\uB0A2\uB0A8\uB0A9\uB0AB" +
                "\uB0AC\uB0AD\uB0AE\uB0AF\uB0B1\uB0B3\uB0B4\uB0B5" +
                "\uB0B8\uB0BC\uB0C4\uB0C5\uB0C7\uB0C8\uB0C9\uB0D0" +
                "\uB0D1\uB0D4\uB0D8\uB0E0\uB0E5\uB108\uB109\uB10B" +
                "\uB10C\uB110\uB112\uB113\uB118\uB119\uB11B\uB11C" +
                "\uB11D\uB123\uB124\uB125\uB128\uB12C\uB134\uB135" +
                "\uB137\uB138\uB139\uB140\uB141\uB144\uB148\uB150" +
                "\uB151\uB154\uB155\uB158\uB15C\uB160\uB178\uB179" +
                "\uB17C\uB180\uB182\uB188\uB189\uB18B\uB18D\uB192" +
                "\uB193\uB194\uB198\uB19C\uB1A8\uB1CC\uB1D0\uB1D4" +
                "\uB1DC\uB1DD\uB1DF\uB1E8\uB1E9\uB1EC\uB1F0\uB1F9" +
                "\uB1FB\uB1FD\uB204\uB205\uB208\uB20B\uB20C\uB214" +
                "\uB215\uB217\uB219\uB220\uB234\uB23C\uB258\uB25C" +
                "\uB260\uB268\uB269\uB274\uB275\uB27C\uB284\uB285" +
                "\uB289\uB290\uB291\uB294\uB298\uB299\uB29A\uB2A0" +
                "\uB2A1\uB2A3\uB2A5\uB2A6\uB2AA\uB2AC\uB2B0\uB2B4" +
                "\uB2C8\uB2C9\uB2CC\uB2D0\uB2D2\uB2D8\uB2D9\uB2DB" +
                "\uB2DD\uB2E2\uB2E4\uB2E5\uB2E6\uB2E8\uB2EB\uB2EC" +
                "\uB2ED\uB2EE\uB2EF\uB2F3\uB2F4\uB2F5\uB2F7\uB2F8" +
                "\uB2F9\uB2FA\uB2FB\uB2FF\uB300\uB301\uB304\uB308" +
                "\uB310\uB311\uB313\uB314\uB315\uB31C\uB354\uB355" +
                "\uB356\uB358\uB35B\uB35C\uB35E\uB35F\uB364\uB365" +
                "\uB367\uB369\uB36B\uB36E\uB370\uB371\uB374\uB378" +
                "\uB380\uB381\uB383\uB384\uB385\uB38C\uB390\uB394" +
                "\uB3A0\uB3A1\uB3A8\uB3AC\uB3C4\uB3C5\uB3C8\uB3CB" +
                "\uB3CC\uB3CE\uB3D0\uB3D4\uB3D5\uB3D7\uB3D9\uB3DB" +
                "\uB3DD\uB3E0\uB3E4\uB3E8\uB3FC\uB410\uB418\uB41C" +
                "\uB420\uB428\uB429\uB42B\uB434\uB450\uB451\uB454" +
                "\uB458\uB460\uB461\uB463\uB465\uB46C\uB480\uB488" +
                "\uB49D\uB4A4\uB4A8\uB4AC\uB4B5\uB4B7\uB4B9\uB4C0" +
                "\uB4C4\uB4C8\uB4D0\uB4D5\uB4DC\uB4DD\uB4E0\uB4E3" +
                "\uB4E4\uB4E6\uB4EC\uB4ED\uB4EF\uB4F1\uB4F8\uB514" +
                "\uB515\uB518\uB51B\uB51C\uB524\uB525\uB527\uB528" +
                "\uB529\uB52A\uB530\uB531\uB534\uB538\uB540\uB541" +
                "\uB543\uB544\uB545\uB54B\uB54C\uB54D\uB550\uB554" +
                "\uB55C\uB55D\uB55F\uB560\uB561\uB5A0\uB5A1\uB5A4" +
                "\uB5A8\uB5AA\uB5AB\uB5B0\uB5B1\uB5B3\uB5B4\uB5B5" +
                "\uB5BB\uB5BC\uB5BD\uB5C0\uB5C4\uB5CC\uB5CD\uB5CF" +
                "\uB5D0\uB5D1\uB5D8\uB5EC\uB610\uB611\uB614\uB618" +
                "\uB625\uB62C\uB634\uB648\uB664\uB668\uB69C\uB69D" +
                "\uB6A0\uB6A4\uB6AB\uB6AC\uB6B1\uB6D4\uB6F0\uB6F4" +
                "\uB6F8\uB700\uB701\uB705\uB728\uB729\uB72C\uB72F" +
                "\uB730\uB738\uB739\uB73B\uB744\uB748\uB74C\uB754" +
                "\uB755\uB760\uB764\uB768\uB770\uB771\uB773\uB775" +
                "\uB77C\uB77D\uB780\uB784\uB78C\uB78D\uB78F\uB790" +
                "\uB791\uB792\uB796\uB797\uB798\uB799\uB79C\uB7A0" +
                "\uB7A8\uB7A9\uB7AB\uB7AC\uB7AD\uB7B4\uB7B5\uB7B8" +
                "\uB7C7\uB7C9\uB7EC\uB7ED\uB7F0\uB7F4\uB7FC\uB7FD" +
                "\uB7FF\uB800\uB801\uB807\uB808\uB809\uB80C\uB810" +
                "\uB818\uB819\uB81B\uB81D\uB824\uB825\uB828\uB82C" +
                "\uB834\uB835\uB837\uB838\uB839\uB840\uB844\uB851" +
                "\uB853\uB85C\uB85D\uB860\uB864\uB86C\uB86D\uB86F" +
                "\uB871\uB878\uB87C\uB88D\uB8A8\uB8B0\uB8B4\uB8B8" +
                "\uB8C0\uB8C1\uB8C3\uB8C5\uB8CC\uB8D0\uB8D4\uB8DD" +
                "\uB8DF\uB8E1\uB8E8\uB8E9\uB8EC\uB8F0\uB8F8\uB8F9" +
                "\uB8FB\uB8FD\uB904\uB918\uB920\uB93C\uB93D\uB940" +
                "\uB944\uB94C\uB94F\uB951\uB958\uB959\uB95C\uB960" +
                "\uB968\uB969\uB96B\uB96D\uB974\uB975\uB978\uB97C" +
                "\uB984\uB985\uB987\uB989\uB98A\uB98D\uB98E\uB9AC" +
                "\uB9AD\uB9B0\uB9B4\uB9BC\uB9BD\uB9BF\uB9C1\uB9C8" +
                "\uB9C9\uB9CC\uB9CE\uB9CF\uB9D0\uB9D1\uB9D2\uB9D8" +
                "\uB9D9\uB9DB\uB9DD\uB9DE\uB9E1\uB9E3\uB9E4\uB9E5" +
                "\uB9E8\uB9EC\uB9F4\uB9F5\uB9F7\uB9F8\uB9F9\uB9FA" +
                "\uBA00\uBA01\uBA08\uBA15\uBA38\uBA39\uBA3C\uBA40" +
                "\uBA42\uBA48\uBA49\uBA4B\uBA4D\uBA4E\uBA53\uBA54" +
                "\uBA55\uBA58\uBA5C\uBA64\uBA65\uBA67\uBA68\uBA69" +
                "\uBA70\uBA71\uBA74\uBA78\uBA83\uBA84\uBA85\uBA87" +
                "\uBA8C\uBAA8\uBAA9\uBAAB\uBAAC\uBAB0\uBAB2\uBAB8" +
                "\uBAB9\uBABB\uBABD\uBAC4\uBAC8\uBAD8\uBAD9\uBAFC" +
                "\uBB00\uBB04\uBB0D\uBB0F\uBB11\uBB18\uBB1C\uBB20" +
                "\uBB29\uBB2B\uBB34\uBB35\uBB36\uBB38\uBB3B\uBB3C" +
                "\uBB3D\uBB3E\uBB44\uBB45\uBB47\uBB49\uBB4D\uBB4F" +
                "\uBB50\uBB54\uBB58\uBB61\uBB63\uBB6C\uBB88\uBB8C" +
                "\uBB90\uBBA4\uBBA8\uBBAC\uBBB4\uBBB7\uBBC0\uBBC4" +
                "\uBBC8\uBBD0\uBBD3\uBBF8\uBBF9\uBBFC\uBBFF\uBC00" +
                "\uBC02\uBC08\uBC09\uBC0B\uBC0C\uBC0D\uBC0F\uBC11" +
                "\uBC14\uBC15\uBC16\uBC17\uBC18\uBC1B\uBC1C\uBC1D" +
                "\uBC1E\uBC1F\uBC24\uBC25\uBC27\uBC29\uBC2D\uBC30" +
                "\uBC31\uBC34\uBC38\uBC40\uBC41\uBC43\uBC44\uBC45" +
                "\uBC49\uBC4C\uBC4D\uBC50\uBC5D\uBC84\uBC85\uBC88" +
                "\uBC8B\uBC8C\uBC8E\uBC94\uBC95\uBC97\uBC99\uBC9A" +
                "\uBCA0\uBCA1\uBCA4\uBCA7\uBCA8\uBCB0\uBCB1\uBCB3" +
                "\uBCB4\uBCB5\uBCBC\uBCBD\uBCC0\uBCC4\uBCCD\uBCCF" +
                "\uBCD0\uBCD1\uBCD5\uBCD8\uBCDC\uBCF4\uBCF5\uBCF6" +
                "\uBCF8\uBCFC\uBD04\uBD05\uBD07\uBD09\uBD10\uBD14" +
                "\uBD24\uBD2C\uBD40\uBD48\uBD49\uBD4C\uBD50\uBD58" +
                "\uBD59\uBD64\uBD68\uBD80\uBD81\uBD84\uBD87\uBD88" +
                "\uBD89\uBD8A\uBD90\uBD91\uBD93\uBD95\uBD99\uBD9A" +
                "\uBD9C\uBDA4\uBDB0\uBDB8\uBDD4\uBDD5\uBDD8\uBDDC" +
                "\uBDE9\uBDF0\uBDF4\uBDF8\uBE00\uBE03\uBE05\uBE0C" +
                "\uBE0D\uBE10\uBE14\uBE1C\uBE1D\uBE1F\uBE44\uBE45" +
                "\uBE48\uBE4C\uBE4E\uBE54\uBE55\uBE57\uBE59\uBE5A" +
                "\uBE5B\uBE60\uBE61\uBE64\uBE68\uBE6A\uBE70\uBE71" +
                "\uBE73\uBE74\uBE75\uBE7B\uBE7C\uBE7D\uBE80\uBE84" +
                "\uBE8C\uBE8D\uBE8F\uBE90\uBE91\uBE98\uBE99\uBEA8" +
                "\uBED0\uBED1\uBED4\uBED7\uBED8\uBEE0\uBEE3\uBEE4" +
                "\uBEE5\uBEEC\uBF01\uBF08\uBF09\uBF18\uBF19\uBF1B" +
                "\uBF1C\uBF1D\uBF40\uBF41\uBF44\uBF48\uBF50\uBF51" +
                "\uBF55\uBF94\uBFB0\uBFC5\uBFCC\uBFCD\uBFD0\uBFD4" +
                "\uBFDC\uBFDF\uBFE1\uC03C\uC051\uC058\uC05C\uC060" +
                "\uC068\uC069\uC090\uC091\uC094\uC098\uC0A0\uC0A1" +
                "\uC0A3\uC0A5\uC0AC\uC0AD\uC0AF\uC0B0\uC0B3\uC0B4" +
                "\uC0B5\uC0B6\uC0BC\uC0BD\uC0BF\uC0C0\uC0C1\uC0C5" +
                "\uC0C8\uC0C9\uC0CC\uC0D0\uC0D8\uC0D9\uC0DB\uC0DC" +
                "\uC0DD\uC0E4\uC0E5\uC0E8\uC0EC\uC0F4\uC0F5\uC0F7" +
                "\uC0F9\uC100\uC104\uC108\uC110\uC115\uC11C\uC11D" +
                "\uC11E\uC11F\uC120\uC123\uC124\uC126\uC127\uC12C" +
                "\uC12D\uC12F\uC130\uC131\uC136\uC138\uC139\uC13C" +
                "\uC140\uC148\uC149\uC14B\uC14C\uC14D\uC154\uC155" +
                "\uC158\uC15C\uC164\uC165\uC167\uC168\uC169\uC170" +
                "\uC174\uC178\uC185\uC18C\uC18D\uC18E\uC190\uC194" +
                "\uC196\uC19C\uC19D\uC19F\uC1A1\uC1A5\uC1A8\uC1A9" +
                "\uC1AC\uC1B0\uC1BD\uC1C4\uC1C8\uC1CC\uC1D4\uC1D7" +
                "\uC1D8\uC1E0\uC1E4\uC1E8\uC1F0\uC1F1\uC1F3\uC1FC" +
                "\uC1FD\uC200\uC204\uC20C\uC20D\uC20F\uC211\uC218" +
                "\uC219\uC21C\uC21F\uC220\uC228\uC229\uC22B\uC22D" +
                "\uC22F\uC231\uC232\uC234\uC248\uC250\uC251\uC254" +
                "\uC258\uC260\uC265\uC26C\uC26D\uC270\uC274\uC27C" +
                "\uC27D\uC27F\uC281\uC288\uC289\uC290\uC298\uC29B" +
                "\uC29D\uC2A4\uC2A5\uC2A8\uC2AC\uC2AD\uC2B4\uC2B5" +
                "\uC2B7\uC2B9\uC2DC\uC2DD\uC2E0\uC2E3\uC2E4\uC2EB" +
                "\uC2EC\uC2ED\uC2EF\uC2F1\uC2F6\uC2F8\uC2F9\uC2FB" +
                "\uC2FC\uC300\uC308\uC309\uC30C\uC30D\uC313\uC314" +
                "\uC315\uC318\uC31C\uC324\uC325\uC328\uC329\uC345" +
                "\uC368\uC369\uC36C\uC370\uC372\uC378\uC379\uC37C" +
                "\uC37D\uC384\uC388\uC38C\uC3C0\uC3D8\uC3D9\uC3DC" +
                "\uC3DF\uC3E0\uC3E2\uC3E8\uC3E9\uC3ED\uC3F4\uC3F5" +
                "\uC3F8\uC408\uC410\uC424\uC42C\uC430\uC434\uC43C" +
                "\uC43D\uC448\uC464\uC465\uC468\uC46C\uC474\uC475" +
                "\uC479\uC480\uC494\uC49C\uC4B8\uC4BC\uC4E9\uC4F0" +
                "\uC4F1\uC4F4\uC4F8\uC4FA\uC4FF\uC500\uC501\uC50C" +
                "\uC510\uC514\uC51C\uC528\uC529\uC52C\uC530\uC538" +
                "\uC539\uC53B\uC53D\uC544\uC545\uC548\uC549\uC54A" +
                "\uC54C\uC54D\uC54E\uC553\uC554\uC555\uC557\uC558" +
                "\uC559\uC55D\uC55E\uC560\uC561\uC564\uC568\uC570" +
                "\uC571\uC573\uC574\uC575\uC57C\uC57D\uC580\uC584" +
                "\uC587\uC58C\uC58D\uC58F\uC591\uC595\uC597\uC598" +
                "\uC59C\uC5A0\uC5A9\uC5B4\uC5B5\uC5B8\uC5B9\uC5BB" +
                "\uC5BC\uC5BD\uC5BE\uC5C4\uC5C5\uC5C6\uC5C7\uC5C8" +
                "\uC5C9\uC5CA\uC5CC\uC5CE\uC5D0\uC5D1\uC5D4\uC5D8" +
                "\uC5E0\uC5E1\uC5E3\uC5E5\uC5EC\uC5ED\uC5EE\uC5F0" +
                "\uC5F4\uC5F6\uC5F7\uC5FC\uC5FD\uC5FE\uC5FF\uC600" +
                "\uC601\uC605\uC606\uC607\uC608\uC60C\uC610\uC618" +
                "\uC619\uC61B\uC61C\uC624\uC625\uC628\uC62C\uC62D" +
                "\uC62E\uC630\uC633\uC634\uC635\uC637\uC639\uC63B" +
                "\uC640\uC641\uC644\uC648\uC650\uC651\uC653\uC654" +
                "\uC655\uC65C\uC65D\uC660\uC66C\uC66F\uC671\uC678" +
                "\uC679\uC67C\uC680\uC688\uC689\uC68B\uC68D\uC694" +
                "\uC695\uC698\uC69C\uC6A4\uC6A5\uC6A7\uC6A9\uC6B0" +
                "\uC6B1\uC6B4\uC6B8\uC6B9\uC6BA\uC6C0\uC6C1\uC6C3" +
                "\uC6C5\uC6CC\uC6CD\uC6D0\uC6D4\uC6DC\uC6DD\uC6E0" +
                "\uC6E1\uC6E8\uC6E9\uC6EC\uC6F0\uC6F8\uC6F9\uC6FD" +
                "\uC704\uC705\uC708\uC70C\uC714\uC715\uC717\uC719" +
                "\uC720\uC721\uC724\uC728\uC730\uC731\uC733\uC735" +
                "\uC737\uC73C\uC73D\uC740\uC744\uC74A\uC74C\uC74D" +
                "\uC74F\uC751\uC752\uC753\uC754\uC755\uC756\uC757" +
                "\uC758\uC75C\uC760\uC768\uC76B\uC774\uC775\uC778" +
                "\uC77C\uC77D\uC77E\uC783\uC784\uC785\uC787\uC788" +
                "\uC789\uC78A\uC78E\uC790\uC791\uC794\uC796\uC797" +
                "\uC798\uC79A\uC7A0\uC7A1\uC7A3\uC7A4\uC7A5\uC7A6" +
                "\uC7AC\uC7AD\uC7B0\uC7B4\uC7BC\uC7BD\uC7BF\uC7C0" +
                "\uC7C1\uC7C8\uC7C9\uC7CC\uC7CE\uC7D0\uC7D8\uC7DD" +
                "\uC7E4\uC7E8\uC7EC\uC800\uC801\uC804\uC808\uC80A" +
                "\uC810\uC811\uC813\uC815\uC816\uC81C\uC81D\uC820" +
                "\uC824\uC82C\uC82D\uC82F\uC831\uC838\uC83C\uC840" +
                "\uC848\uC849\uC84C\uC84D\uC854\uC870\uC871\uC874" +
                "\uC878\uC87A\uC880\uC881\uC883\uC885\uC886\uC887" +
                "\uC88B\uC88C\uC88D\uC894\uC89D\uC89F\uC8A1\uC8A8" +
                "\uC8BC\uC8BD\uC8C4\uC8C8\uC8CC\uC8D4\uC8D5\uC8D7" +
                "\uC8D9\uC8E0\uC8E1\uC8E4\uC8F5\uC8FC\uC8FD\uC900" +
                "\uC904\uC905\uC906\uC90C\uC90D\uC90F\uC911\uC918" +
                "\uC92C\uC934\uC950\uC951\uC954\uC958\uC960\uC961" +
                "\uC963\uC96C\uC970\uC974\uC97C\uC988\uC989\uC98C" +
                "\uC990\uC998\uC999\uC99B\uC99D\uC9C0\uC9C1\uC9C4" +
                "\uC9C7\uC9C8\uC9CA\uC9D0\uC9D1\uC9D3\uC9D5\uC9D6" +
                "\uC9D9\uC9DA\uC9DC\uC9DD\uC9E0\uC9E2\uC9E4\uC9E7" +
                "\uC9EC\uC9ED\uC9EF\uC9F0\uC9F1\uC9F8\uC9F9\uC9FC" +
                "\uCA00\uCA08\uCA09\uCA0B\uCA0C\uCA0D\uCA14\uCA18" +
                "\uCA29\uCA4C\uCA4D\uCA50\uCA54\uCA5C\uCA5D\uCA5F" +
                "\uCA60\uCA61\uCA68\uCA7D\uCA84\uCA98\uCABC\uCABD" +
                "\uCAC0\uCAC4\uCACC\uCACD\uCACF\uCAD1\uCAD3\uCAD8" +
                "\uCAD9\uCAE0\uCAEC\uCAF4\uCB08\uCB10\uCB14\uCB18" +
                "\uCB20\uCB21\uCB41\uCB48\uCB49\uCB4C\uCB50\uCB58" +
                "\uCB59\uCB5D\uCB64\uCB78\uCB79\uCB9C\uCBB8\uCBD4" +
                "\uCBE4\uCBE7\uCBE9\uCC0C\uCC0D\uCC10\uCC14\uCC1C" +
                "\uCC1D\uCC21\uCC22\uCC27\uCC28\uCC29\uCC2C\uCC2E" +
                "\uCC30\uCC38\uCC39\uCC3B\uCC3C\uCC3D\uCC3E\uCC44" +
                "\uCC45\uCC48\uCC4C\uCC54\uCC55\uCC57\uCC58\uCC59" +
                "\uCC60\uCC64\uCC66\uCC68\uCC70\uCC75\uCC98\uCC99" +
                "\uCC9C\uCCA0\uCCA8\uCCA9\uCCAB\uCCAC\uCCAD\uCCB4" +
                "\uCCB5\uCCB8\uCCBC\uCCC4\uCCC5\uCCC7\uCCC9\uCCD0" +
                "\uCCD4\uCCE4\uCCEC\uCCF0\uCD01\uCD08\uCD09\uCD0C" +
                "\uCD10\uCD18\uCD19\uCD1B\uCD1D\uCD24\uCD28\uCD2C" +
                "\uCD39\uCD5C\uCD60\uCD64\uCD6C\uCD6D\uCD6F\uCD71" +
                "\uCD78\uCD88\uCD94\uCD95\uCD98\uCD9C\uCDA4\uCDA5" +
                "\uCDA7\uCDA9\uCDB0\uCDC4\uCDCC\uCDD0\uCDE8\uCDEC" +
                "\uCDF0\uCDF8\uCDF9\uCDFB\uCDFD\uCE04\uCE08\uCE0C" +
                "\uCE14\uCE19\uCE20\uCE21\uCE24\uCE28\uCE30\uCE31" +
                "\uCE33\uCE35\uCE58\uCE59\uCE5C\uCE5F\uCE60\uCE61" +
                "\uCE68\uCE69\uCE6B\uCE6D\uCE74\uCE75\uCE78\uCE7C" +
                "\uCE84\uCE85\uCE87\uCE89\uCE90\uCE91\uCE94\uCE98" +
                "\uCEA0\uCEA1\uCEA3\uCEA4\uCEA5\uCEAC\uCEAD\uCEC1" +
                "\uCEE4\uCEE5\uCEE8\uCEEB\uCEEC\uCEF4\uCEF5\uCEF7" +
                "\uCEF8\uCEF9\uCF00\uCF01\uCF04\uCF08\uCF10\uCF11" +
                "\uCF13\uCF15\uCF1C\uCF20\uCF24\uCF2C\uCF2D\uCF2F" +
                "\uCF30\uCF31\uCF38\uCF54\uCF55\uCF58\uCF5C\uCF64" +
                "\uCF65\uCF67\uCF69\uCF70\uCF71\uCF74\uCF78\uCF80" +
                "\uCF85\uCF8C\uCFA1\uCFA8\uCFB0\uCFC4\uCFE0\uCFE1" +
                "\uCFE4\uCFE8\uCFF0\uCFF1\uCFF3\uCFF5\uCFFC\uD000" +
                "\uD004\uD011\uD018\uD02D\uD034\uD035\uD038\uD03C" +
                "\uD044\uD045\uD047\uD049\uD050\uD054\uD058\uD060" +
                "\uD06C\uD06D\uD070\uD074\uD07C\uD07D\uD081\uD0A4" +
                "\uD0A5\uD0A8\uD0AC\uD0B4\uD0B5\uD0B7\uD0B9\uD0C0" +
                "\uD0C1\uD0C4\uD0C8\uD0C9\uD0D0\uD0D1\uD0D3\uD0D4" +
                "\uD0D5\uD0DC\uD0DD\uD0E0\uD0E4\uD0EC\uD0ED\uD0EF" +
                "\uD0F0\uD0F1\uD0F8\uD10D\uD130\uD131\uD134\uD138" +
                "\uD13A\uD140\uD141\uD143\uD144\uD145\uD14C\uD14D" +
                "\uD150\uD154\uD15C\uD15D\uD15F\uD161\uD168\uD16C" +
                "\uD17C\uD184\uD188\uD1A0\uD1A1\uD1A4\uD1A8\uD1B0" +
                "\uD1B1\uD1B3\uD1B5\uD1BA\uD1BC\uD1C0\uD1D8\uD1F4" +
                "\uD1F8\uD207\uD209\uD210\uD22C\uD22D\uD230\uD234" +
                "\uD23C\uD23D\uD23F\uD241\uD248\uD25C\uD264\uD280" +
                "\uD281\uD284\uD288\uD290\uD291\uD295\uD29C\uD2A0" +
                "\uD2A4\uD2AC\uD2B1\uD2B8\uD2B9\uD2BC\uD2BF\uD2C0" +
                "\uD2C2\uD2C8\uD2C9\uD2CB\uD2D4\uD2D8\uD2DC\uD2E4" +
                "\uD2E5\uD2F0\uD2F1\uD2F4\uD2F8\uD300\uD301\uD303" +
                "\uD305\uD30C\uD30D\uD30E\uD310\uD314\uD316\uD31C" +
                "\uD31D\uD31F\uD320\uD321\uD325\uD328\uD329\uD32C" +
                "\uD330\uD338\uD339\uD33B\uD33C\uD33D\uD344\uD345" +
                "\uD37C\uD37D\uD380\uD384\uD38C\uD38D\uD38F\uD390" +
                "\uD391\uD398\uD399\uD39C\uD3A0\uD3A8\uD3A9\uD3AB" +
                "\uD3AD\uD3B4\uD3B8\uD3BC\uD3C4\uD3C5\uD3C8\uD3C9" +
                "\uD3D0\uD3D8\uD3E1\uD3E3\uD3EC\uD3ED\uD3F0\uD3F4" +
                "\uD3FC\uD3FD\uD3FF\uD401\uD408\uD41D\uD440\uD444" +
                "\uD45C\uD460\uD464\uD46D\uD46F\uD478\uD479\uD47C" +
                "\uD47F\uD480\uD482\uD488\uD489\uD48B\uD48D\uD494" +
                "\uD4A9\uD4CC\uD4D0\uD4D4\uD4DC\uD4DF\uD4E8\uD4EC" +
                "\uD4F0\uD4F8\uD4FB\uD4FD\uD504\uD508\uD50C\uD514" +
                "\uD515\uD517\uD53C\uD53D\uD540\uD544\uD54C\uD54D" +
                "\uD54F\uD551\uD558\uD559\uD55C\uD560\uD565\uD568" +
                "\uD569\uD56B\uD56D\uD574\uD575\uD578\uD57C\uD584" +
                "\uD585\uD587\uD588\uD589\uD590\uD5A5\uD5C8\uD5C9" +
                "\uD5CC\uD5D0\uD5D2\uD5D8\uD5D9\uD5DB\uD5DD\uD5E4" +
                "\uD5E5\uD5E8\uD5EC\uD5F4\uD5F5\uD5F7\uD5F9\uD600" +
                "\uD601\uD604\uD608\uD610\uD611\uD613\uD614\uD615" +
                "\uD61C\uD620\uD624\uD62D\uD638\uD639\uD63C\uD640" +
                "\uD645\uD648\uD649\uD64B\uD64D\uD651\uD654\uD655" +
                "\uD658\uD65C\uD667\uD669\uD670\uD671\uD674\uD683" +
                "\uD685\uD68C\uD68D\uD690\uD694\uD69D\uD69F\uD6A1" +
                "\uD6A8\uD6AC\uD6B0\uD6B9\uD6BB\uD6C4\uD6C5\uD6C8" +
                "\uD6CC\uD6D1\uD6D4\uD6D7\uD6D9\uD6E0\uD6E4\uD6E8" +
                "\uD6F0\uD6F5\uD6FC\uD6FD\uD700\uD704\uD711\uD718" +
                "\uD719\uD71C\uD720\uD728\uD729\uD72B\uD72D\uD734" +
                "\uD735\uD738\uD73C\uD744\uD747\uD749\uD750\uD751" +
                "\uD754\uD756\uD757\uD758\uD759\uD760\uD761\uD763" +
                "\uD765\uD769\uD76C\uD770\uD774\uD77C\uD77D\uD781" +
                "\uD788\uD789\uD78C\uD790\uD798\uD799\uD79B\uD79D" +
                "\uE000\uE001\uE002\uE003\uE004\uE005\uE006\uE007" +
                "\uE008\uE009\uE00A\uE00B\uE00C\uE00D\uE00E\uE00F" +
                "\uE010\uE011\uE012\uE013\uE014\uE015\uE016\uE017" +
                "\uE018\uE019\uE01A\uE01B\uE01C\uE01D\uE01E\uE01F" +
                "\uE020\uE021\uE022\uE023\uE024\uE025\uE026\uE027" +
                "\uE028\uE029\uE02A\uE02B\uE02C\uE02D\uE02E\uE02F" +
                "\uE030\uE031\uE032\uE033\uE034\uE035\uE036\uE037" +
                "\uE038\uE039\uE03A\uE03B\uE03C\uE03D\uE03E\uE03F" +
                "\uE040\uE041\uE042\uE043\uE044\uE045\uE046\uE047" +
                "\uE048\uE049\uE04A\uE04B\uE04C\uE04D\uE04E\uE04F" +
                "\uE050\uE051\uE052\uE053\uE054\uE055\uE056\uE057" +
                "\uE058\uE059\uE05A\uE05B\uE05C\uE05D\u4F3D\u4F73" +
                "\u5047\u50F9\u52A0\u53EF\u5475\u54E5\u5609\u5AC1" +
                "\u5BB6\u6687\u67B6\u67B7\u67EF\u6B4C\u73C2\u75C2" +
                "\u7A3C\u82DB\u8304\u8857\u8888\u8A36\u8CC8\u8DCF" +
                "\u8EFB\u8FE6\u99D5\u523B\u5374\u5404\u606A\u6164" +
                "\u6BBC\u73CF\u811A\u89BA\u89D2\u95A3\u4F83\u520A" +
                "\u58BE\u5978\u59E6\u5E72\u5E79\u61C7\u63C0\u6746" +
                "\u67EC\u687F\u6F97\u764E\u770B\u78F5\u7A08\u7AFF" +
                "\u7C21\u809D\u826E\u8271\u8AEB\u9593\u4E6B\u559D" +
                "\u66F7\u6E34\u78A3\u7AED\u845B\u8910\u874E\u97A8" +
                "\u52D8\u574E\u582A\u5D4C\u611F\u61BE\u6221\u6562" +
                "\u67D1\u6A44\u6E1B\u7518\u75B3\u76E3\u77B0\u7D3A" +
                "\u90AF\u9451\u9452\u9F95\u5323\u5CAC\u7532\u80DB" +
                "\u9240\u9598\u525B\u5808\u59DC\u5CA1\u5D17\u5EB7" +
                "\u5F3A\u5F4A\u6177\u6C5F\u757A\u7586\u7CE0\u7D73" +
                "\u7DB1\u7F8C\u8154\u8221\u8591\u8941\u8B1B\u92FC" +
                "\u964D\u9C47\u4ECB\u4EF7\u500B\u51F1\u584F\u6137" +
                "\u613E\u6168\u6539\u69EA\u6F11\u75A5\u7686\u76D6" +
                "\u7B87\u82A5\u84CB\uF900\u93A7\u958B\u5580\u5BA2" +
                "\u5751\uF901\u7CB3\u7FB9\u91B5\u5028\u53BB\u5C45" +
                "\u5DE8\u62D2\u636E\u64DA\u64E7\u6E20\u70AC\u795B" +
                "\u8DDD\u8E1E\uF902\u907D\u9245\u92F8\u4E7E\u4EF6" +
                "\u5065\u5DFE\u5EFA\u6106\u6957\u8171\u8654\u8E47" +
                "\u9375\u9A2B\u4E5E\u5091\u6770\u6840\u5109\u528D" +
                "\u5292\u6AA2\u77BC\u9210\u9ED4\u52AB\u602F\u8FF2" +
                "\u5048\u61A9\u63ED\u64CA\u683C\u6A84\u6FC0\u8188" +
                "\u89A1\u9694\u5805\u727D\u72AC\u7504\u7D79\u7E6D" +
                "\u80A9\u898B\u8B74\u9063\u9D51\u6289\u6C7A\u6F54" +
                "\u7D50\u7F3A\u8A23\u517C\u614A\u7B9D\u8B19\u9257" +
                "\u938C\u4EAC\u4FD3\u501E\u50BE\u5106\u52C1\u52CD" +
                "\u537F\u5770\u5883\u5E9A\u5F91\u6176\u61AC\u64CE" +
                "\u656C\u666F\u66BB\u66F4\u6897\u6D87\u7085\u70F1" +
                "\u749F\u74A5\u74CA\u75D9\u786C\u78EC\u7ADF\u7AF6" +
                "\u7D45\u7D93\u8015\u803F\u811B\u8396\u8B66\u8F15" +
                "\u9015\u93E1\u9803\u9838\u9A5A\u9BE8\u4FC2\u5553" +
                "\u583A\u5951\u5B63\u5C46\u60B8\u6212\u6842\u68B0" +
                "\u68E8\u6EAA\u754C\u7678\u78CE\u7A3D\u7CFB\u7E6B" +
                "\u7E7C\u8A08\u8AA1\u8C3F\u968E\u9DC4\u53E4\u53E9" +
                "\u544A\u5471\u56FA\u59D1\u5B64\u5C3B\u5EAB\u62F7" +
                "\u6537\u6545\u6572\u66A0\u67AF\u69C1\u6CBD\u75FC" +
                "\u7690\u777E\u7A3F\u7F94\u8003\u80A1\u818F\u82E6" +
                "\u82FD\u83F0\u85C1\u8831\u88B4\u8AA5\uF903\u8F9C" +
                "\u932E\u96C7\u9867\u9AD8\u9F13\u54ED\u659B\u66F2" +
                "\u688F\u7A40\u8C37\u9D60\u56F0\u5764\u5D11\u6606" +
                "\u68B1\u68CD\u6EFE\u7428\u889E\u9BE4\u6C68\uF904" +
                "\u9AA8\u4F9B\u516C\u5171\u529F\u5B54\u5DE5\u6050" +
                "\u606D\u62F1\u63A7\u653B\u73D9\u7A7A\u86A3\u8CA2" +
                "\u978F\u4E32\u5BE1\u6208\u679C\u74DC\u79D1\u83D3" +
                "\u8A87\u8AB2\u8DE8\u904E\u934B\u9846\u5ED3\u69E8" +
                "\u85FF\u90ED\uF905\u51A0\u5B98\u5BEC\u6163\u68FA" +
                "\u6B3E\u704C\u742F\u74D8\u7BA1\u7F50\u83C5\u89C0" +
                "\u8CAB\u95DC\u9928\u522E\u605D\u62EC\u9002\u4F8A" +
                "\u5149\u5321\u58D9\u5EE3\u66E0\u6D38\u709A\u72C2" +
                "\u73D6\u7B50\u80F1\u945B\u5366\u639B\u7F6B\u4E56" +
                "\u5080\u584A\u58DE\u602A\u6127\u62D0\u69D0\u9B41" +
                "\u5B8F\u7D18\u80B1\u8F5F\u4EA4\u50D1\u54AC\u55AC" +
                "\u5B0C\u5DA0\u5DE7\u652A\u654E\u6821\u6A4B\u72E1" +
                "\u768E\u77EF\u7D5E\u7FF9\u81A0\u854E\u86DF\u8F03" +
                "\u8F4E\u90CA\u9903\u9A55\u9BAB\u4E18\u4E45\u4E5D" +
                "\u4EC7\u4FF1\u5177\u52FE\u5340\u53E3\u53E5\u548E" +
                "\u5614\u5775\u57A2\u5BC7\u5D87\u5ED0\u61FC\u62D8" +
                "\u6551\u67B8\u67E9\u69CB\u6B50\u6BC6\u6BEC\u6C42" +
                "\u6E9D\u7078\u72D7\u7396\u7403\u77BF\u77E9\u7A76" +
                "\u7D7F\u8009\u81FC\u8205\u820A\u82DF\u8862\u8B33" +
                "\u8CFC\u8EC0\u9011\u90B1\u9264\u92B6\u99D2\u9A45" +
                "\u9CE9\u9DD7\u9F9C\u570B\u5C40\u83CA\u97A0\u97AB" +
                "\u9EB4\u541B\u7A98\u7FA4\u88D9\u8ECD\u90E1\u5800" +
                "\u5C48\u6398\u7A9F\u5BAE\u5F13\u7A79\u7AAE\u828E" +
                "\u8EAC\u5026\u5238\u52F8\u5377\u5708\u62F3\u6372" +
                "\u6B0A\u6DC3\u7737\u53A5\u7357\u8568\u8E76\u95D5" +
                "\u673A\u6AC3\u6F70\u8A6D\u8ECC\u994B\uF906\u6677" +
                "\u6B78\u8CB4\u9B3C\uF907\u53EB\u572D\u594E\u63C6" +
                "\u69FB\u73EA\u7845\u7ABA\u7AC5\u7CFE\u8475\u898F" +
                "\u8D73\u9035\u95A8\u52FB\u5747\u7547\u7B60\u83CC" +
                "\u921E\uF908\u6A58\u514B\u524B\u5287\u621F\u68D8" +
                "\u6975\u9699\u50C5\u52A4\u52E4\u61C3\u65A4\u6839" +
                "\u69FF\u747E\u7B4B\u82B9\u83EB\u89B2\u8B39\u8FD1" +
                "\u9949\uF909\u4ECA\u5997\u64D2\u6611\u6A8E\u7434" +
                "\u7981\u79BD\u82A9\u887E\u887F\u895F\uF90A\u9326" +
                "\u4F0B\u53CA\u6025\u6271\u6C72\u7D1A\u7D66\u4E98" +
                "\u5162\u77DC\u80AF\u4F01\u4F0E\u5176\u5180\u55DC" +
                "\u5668\u573B\u57FA\u57FC\u5914\u5947\u5993\u5BC4" +
                "\u5C90\u5D0E\u5DF1\u5E7E\u5FCC\u6280\u65D7\u65E3" +
                "\u671E\u671F\u675E\u68CB\u68C4\u6A5F\u6B3A\u6C23" +
                "\u6C7D\u6C82\u6DC7\u7398\u7426\u742A\u7482\u74A3" +
                "\u7578\u757F\u7881\u78EF\u7941\u7947\u7948\u797A" +
                "\u7B95\u7D00\u7DBA\u7F88\u8006\u802D\u808C\u8A18" +
                "\u8B4F\u8C48\u8D77\u9321\u9324\u98E2\u9951\u9A0E" +
                "\u9A0F\u9A65\u9E92\u7DCA\u4F76\u5409\u62EE\u6854" +
                "\u91D1\u55AB\u513A\uF90B\uF90C\u5A1C\u61E6\uF90D" +
                "\u62CF\u62FF\uF90E\uF90F\uF910\uF911\uF912\uF913" +
                "\u90A3\uF914\uF915\uF916\uF917\uF918\u8AFE\uF919" +
                "\uF91A\uF91B\uF91C\u6696\uF91D\u7156\uF91E\uF91F" +
                "\u96E3\uF920\u634F\u637A\u5357\uF921\u678F\u6960" +
                "\u6E73\uF922\u7537\uF923\uF924\uF925\u7D0D\uF926" +
                "\uF927\u8872\u56CA\u5A18\uF928\uF929\uF92A\uF92B" +
                "\uF92C\u4E43\uF92D\u5167\u5948\u67F0\u8010\uF92E" +
                "\u5973\u5E74\u649A\u79CA\u5FF5\u606C\u62C8\u637B" +
                "\u5BE7\u5BD7\u52AA\uF92F\u5974\u5F29\u6012\uF930" +
                "\uF931\uF932\u7459\uF933\uF934\uF935\uF936\uF937" +
                "\uF938\u99D1\uF939\uF93A\uF93B\uF93C\uF93D\uF93E" +
                "\uF93F\uF940\uF941\uF942\uF943\u6FC3\uF944\uF945" +
                "\u81BF\u8FB2\u60F1\uF946\uF947\u8166\uF948\uF949" +
                "\u5C3F\uF94A\uF94B\uF94C\uF94D\uF94E\uF94F\uF950" +
                "\uF951\u5AE9\u8A25\u677B\u7D10\uF952\uF953\uF954" +
                "\uF955\uF956\uF957\u80FD\uF958\uF959\u5C3C\u6CE5" +
                "\u533F\u6EBA\u591A\u8336\u4E39\u4EB6\u4F46\u55AE" +
                "\u5718\u58C7\u5F56\u65B7\u65E6\u6A80\u6BB5\u6E4D" +
                "\u77ED\u7AEF\u7C1E\u7DDE\u86CB\u8892\u9132\u935B" +
                "\u64BB\u6FBE\u737A\u75B8\u9054\u5556\u574D\u61BA" +
                "\u64D4\u66C7\u6DE1\u6E5B\u6F6D\u6FB9\u75F0\u8043" +
                "\u81BD\u8541\u8983\u8AC7\u8B5A\u931F\u6C93\u7553" +
                "\u7B54\u8E0F\u905D\u5510\u5802\u5858\u5E62\u6207" +
                "\u649E\u68E0\u7576\u7CD6\u87B3\u9EE8\u4EE3\u5788" +
                "\u576E\u5927\u5C0D\u5CB1\u5E36\u5F85\u6234\u64E1" +
                "\u73B3\u81FA\u888B\u8CB8\u968A\u9EDB\u5B85\u5FB7" +
                "\u60B3\u5012\u5200\u5230\u5716\u5835\u5857\u5C0E" +
                "\u5C60\u5CF6\u5D8B\u5EA6\u5F92\u60BC\u6311\u6389" +
                "\u6417\u6843\u68F9\u6AC2\u6DD8\u6E21\u6ED4\u6FE4" +
                "\u71FE\u76DC\u7779\u79B1\u7A3B\u8404\u89A9\u8CED" +
                "\u8DF3\u8E48\u9003\u9014\u9053\u90FD\u934D\u9676" +
                "\u97DC\u6BD2\u7006\u7258\u72A2\u7368\u7763\u79BF" +
                "\u7BE4\u7E9B\u8B80\u58A9\u60C7\u6566\u65FD\u66BE" +
                "\u6C8C\u711E\u71C9\u8C5A\u9813\u4E6D\u7A81\u4EDD" +
                "\u51AC\u51CD\u52D5\u540C\u61A7\u6771\u6850\u68DF" +
                "\u6D1E\u6F7C\u75BC\u77B3\u7AE5\u80F4\u8463\u9285" +
                "\u515C\u6597\u675C\u6793\u75D8\u7AC7\u8373\uF95A" +
                "\u8C46\u9017\u982D\u5C6F\u81C0\u829A\u9041\u906F" +
                "\u920D\u5F97\u5D9D\u6A59\u71C8\u767B\u7B49\u85E4" +
                "\u8B04\u9127\u9A30\u5587\u61F6\uF95B\u7669\u7F85" +
                "\u863F\u87BA\u88F8\u908F\uF95C\u6D1B\u70D9\u73DE" +
                "\u7D61\u843D\uF95D\u916A\u99F1\uF95E\u4E82\u5375" +
                "\u6B04\u6B12\u703E\u721B\u862D\u9E1E\u524C\u8FA3" +
                "\u5D50\u64E5\u652C\u6B16\u6FEB\u7C43\u7E9C\u85CD" +
                "\u8964\u89BD\u62C9\u81D8\u881F\u5ECA\u6717\u6D6A" +
                "\u72FC\u7405\u746F\u8782\u90DE\u4F86\u5D0D\u5FA0" +
                "\u840A\u51B7\u63A0\u7565\u4EAE\u5006\u5169\u51C9" +
                "\u6881\u6A11\u7CAE\u7CB1\u7CE7\u826F\u8AD2\u8F1B" +
                "\u91CF\u4FB6\u5137\u52F5\u5442\u5EEC\u616E\u623E" +
                "\u65C5\u6ADA\u6FFE\u792A\u85DC\u8823\u95AD\u9A62" +
                "\u9A6A\u9E97\u9ECE\u529B\u66C6\u6B77\u701D\u792B" +
                "\u8F62\u9742\u6190\u6200\u6523\u6F23\u7149\u7489" +
                "\u7DF4\u806F\u84EE\u8F26\u9023\u934A\u51BD\u5217" +
                "\u52A3\u6D0C\u70C8\u88C2\u5EC9\u6582\u6BAE\u6FC2" +
                "\u7C3E\u7375\u4EE4\u4F36\u56F9\uF95F\u5CBA\u5DBA" +
                "\u601C\u73B2\u7B2D\u7F9A\u7FCE\u8046\u901E\u9234" +
                "\u96F6\u9748\u9818\u9F61\u4F8B\u6FA7\u79AE\u91B4" +
                "\u96B7\u52DE\uF960\u6488\u64C4\u6AD3\u6F5E\u7018" +
                "\u7210\u76E7\u8001\u8606\u865C\u8DEF\u8F05\u9732" +
                "\u9B6F\u9DFA\u9E75\u788C\u797F\u7DA0\u83C9\u9304" +
                "\u9E7F\u9E93\u8AD6\u58DF\u5F04\u6727\u7027\u74CF" +
                "\u7C60\u807E\u5121\u7028\u7262\u78CA\u8CC2\u8CDA" +
                "\u8CF4\u96F7\u4E86\u50DA\u5BEE\u5ED6\u6599\u71CE" +
                "\u7642\u77AD\u804A\u84FC\u907C\u9B27\u9F8D\u58D8" +
                "\u5A41\u5C62\u6A13\u6DDA\u6F0F\u763B\u7D2F\u7E37" +
                "\u851E\u8938\u93E4\u964B\u5289\u65D2\u67F3\u69B4" +
                "\u6D41\u6E9C\u700F\u7409\u7460\u7559\u7624\u786B" +
                "\u8B2C\u985E\u516D\u622E\u9678\u4F96\u502B\u5D19" +
                "\u6DEA\u7DB8\u8F2A\u5F8B\u6144\u6817\uF961\u9686" +
                "\u52D2\u808B\u51DC\u51CC\u695E\u7A1C\u7DBE\u83F1" +
                "\u9675\u4FDA\u5229\u5398\u540F\u550E\u5C65\u60A7" +
                "\u674E\u68A8\u6D6C\u7281\u72F8\u7406\u7483\uF962" +
                "\u75E2\u7C6C\u7F79\u7FB8\u8389\u88CF\u88E1\u91CC" +
                "\u91D0\u96E2\u9BC9\u541D\u6F7E\u71D0\u7498\u85FA" +
                "\u8EAA\u96A3\u9C57\u9E9F\u6797\u6DCB\u7433\u81E8" +
                "\u9716\u782C\u7ACB\u7B20\u7C92\u6469\u746A\u75F2" +
                "\u78BC\u78E8\u99AC\u9B54\u9EBB\u5BDE\u5E55\u6F20" +
                "\u819C\u83AB\u9088\u4E07\u534D\u5A29\u5DD2\u5F4E" +
                "\u6162\u633D\u6669\u66FC\u6EFF\u6F2B\u7063\u779E" +
                "\u842C\u8513\u883B\u8F13\u9945\u9C3B\u551C\u62B9" +
                "\u672B\u6CAB\u8309\u896A\u977A\u4EA1\u5984\u5FD8" +
                "\u5FD9\u671B\u7DB2\u7F54\u8292\u832B\u83BD\u8F1E" +
                "\u9099\u57CB\u59B9\u5A92\u5BD0\u6627\u679A\u6885" +
                "\u6BCF\u7164\u7F75\u8CB7\u8CE3\u9081\u9B45\u8108" +
                "\u8C8A\u964C\u9A40\u9EA5\u5B5F\u6C13\u731B\u76F2" +
                "\u76DF\u840C\u51AA\u8993\u514D\u5195\u52C9\u68C9" +
                "\u6C94\u7704\u7720\u7DBF\u7DEC\u9762\u9EB5\u6EC5" +
                "\u8511\u51A5\u540D\u547D\u660E\u669D\u6927\u6E9F" +
                "\u76BF\u7791\u8317\u84C2\u879F\u9169\u9298\u9CF4" +
                "\u8882\u4FAE\u5192\u52DF\u59C6\u5E3D\u6155\u6478" +
                "\u6479\u66AE\u67D0\u6A21\u6BCD\u6BDB\u725F\u7261" +
                "\u7441\u7738\u77DB\u8017\u82BC\u8305\u8B00\u8B28" +
                "\u8C8C\u6728\u6C90\u7267\u76EE\u7766\u7A46\u9DA9" +
                "\u6B7F\u6C92\u5922\u6726\u8499\u536F\u5893\u5999" +
                "\u5EDF\u63CF\u6634\u6773\u6E3A\u732B\u7AD7\u82D7" +
                "\u9328\u52D9\u5DEB\u61AE\u61CB\u620A\u62C7\u64AB" +
                "\u65E0\u6959\u6B66\u6BCB\u7121\u73F7\u755D\u7E46" +
                "\u821E\u8302\u856A\u8AA3\u8CBF\u9727\u9D61\u58A8" +
                "\u9ED8\u5011\u520E\u543B\u554F\u6587\u6C76\u7D0A" +
                "\u7D0B\u805E\u868A\u9580\u96EF\u52FF\u6C95\u7269" +
                "\u5473\u5A9A\u5C3E\u5D4B\u5F4C\u5FAE\u672A\u68B6" +
                "\u6963\u6E3C\u6E44\u7709\u7C73\u7F8E\u8587\u8B0E" +
                "\u8FF7\u9761\u9EF4\u5CB7\u60B6\u610D\u61AB\u654F" +
                "\u65FB\u65FC\u6C11\u6CEF\u739F\u73C9\u7DE1\u9594" +
                "\u5BC6\u871C\u8B10\u525D\u535A\u62CD\u640F\u64B2" +
                "\u6734\u6A38\u6CCA\u73C0\u749E\u7B94\u7C95\u7E1B" +
                "\u818A\u8236\u8584\u8FEB\u96F9\u99C1\u4F34\u534A" +
                "\u53CD\u53DB\u62CC\u642C\u6500\u6591\u69C3\u6CEE" +
                "\u6F58\u73ED\u7554\u7622\u76E4\u76FC\u78D0\u78FB" +
                "\u792C\u7D46\u822C\u87E0\u8FD4\u9812\u98EF\u52C3" +
                "\u62D4\u64A5\u6E24\u6F51\u767C\u8DCB\u91B1\u9262" +
                "\u9AEE\u9B43\u5023\u508D\u574A\u59A8\u5C28\u5E47" +
                "\u5F77\u623F\u653E\u65B9\u65C1\u6609\u678B\u699C" +
                "\u6EC2\u78C5\u7D21\u80AA\u8180\u822B\u82B3\u84A1" +
                "\u868C\u8A2A\u8B17\u90A6\u9632\u9F90\u500D\u4FF3" +
                "\uF963\u57F9\u5F98\u62DC\u6392\u676F\u6E43\u7119" +
                "\u76C3\u80CC\u80DA\u88F4\u88F5\u8919\u8CE0\u8F29" +
                "\u914D\u966A\u4F2F\u4F70\u5E1B\u67CF\u6822\u767D" +
                "\u767E\u9B44\u5E61\u6A0A\u7169\u71D4\u756A\uF964" +
                "\u7E41\u8543\u85E9\u98DC\u4F10\u7B4F\u7F70\u95A5" +
                "\u51E1\u5E06\u68B5\u6C3E\u6C4E\u6CDB\u72AF\u7BC4" +
                "\u8303\u6CD5\u743A\u50FB\u5288\u58C1\u64D8\u6A97" +
                "\u74A7\u7656\u78A7\u8617\u95E2\u9739\uF965\u535E" +
                "\u5F01\u8B8A\u8FA8\u8FAF\u908A\u5225\u77A5\u9C49" +
                "\u9F08\u4E19\u5002\u5175\u5C5B\u5E77\u661E\u663A" +
                "\u67C4\u68C5\u70B3\u7501\u75C5\u79C9\u7ADD\u8F27" +
                "\u9920\u9A08\u4FDD\u5821\u5831\u5BF6\u666E\u6B65" +
                "\u6D11\u6E7A\u6F7D\u73E4\u752B\u83E9\u88DC\u8913" +
                "\u8B5C\u8F14\u4F0F\u50D5\u5310\u535C\u5B93\u5FA9" +
                "\u670D\u798F\u8179\u832F\u8514\u8907\u8986\u8F39" +
                "\u8F3B\u99A5\u9C12\u672C\u4E76\u4FF8\u5949\u5C01" +
                "\u5CEF\u5CF0\u6367\u68D2\u70FD\u71A2\u742B\u7E2B" +
                "\u84EC\u8702\u9022\u92D2\u9CF3\u4E0D\u4ED8\u4FEF" +
                "\u5085\u5256\u526F\u5426\u5490\u57E0\u592B\u5A66" +
                "\u5B5A\u5B75\u5BCC\u5E9C\uF966\u6276\u6577\u65A7" +
                "\u6D6E\u6EA5\u7236\u7B26\u7C3F\u7F36\u8150\u8151" +
                "\u819A\u8240\u8299\u83A9\u8A03\u8CA0\u8CE6\u8CFB" +
                "\u8D74\u8DBA\u90E8\u91DC\u961C\u9644\u99D9\u9CE7" +
                "\u5317\u5206\u5429\u5674\u58B3\u5954\u596E\u5FFF" +
                "\u61A4\u626E\u6610\u6C7E\u711A\u76C6\u7C89\u7CDE" +
                "\u7D1B\u82AC\u8CC1\u96F0\uF967\u4F5B\u5F17\u5F7F" +
                "\u62C2\u5D29\u670B\u68DA\u787C\u7E43\u9D6C\u4E15" +
                "\u5099\u5315\u532A\u5351\u5983\u5A62\u5E87\u60B2" +
                "\u618A\u6249\u6279\u6590\u6787\u69A7\u6BD4\u6BD6" +
                "\u6BD7\u6BD8\u6CB8\uF968\u7435\u75FA\u7812\u7891" +
                "\u79D5\u79D8\u7C83\u7DCB\u7FE1\u80A5\u813E\u81C2" +
                "\u83F2\u871A\u88E8\u8AB9\u8B6C\u8CBB\u9119\u975E" +
                "\u98DB\u9F3B\u56AC\u5B2A\u5F6C\u658C\u6AB3\u6BAF" +
                "\u6D5C\u6FF1\u7015\u725D\u73AD\u8CA7\u8CD3\u983B" +
                "\u6191\u6C37\u8058\u9A01\u4E4D\u4E8B\u4E9B\u4ED5" +
                "\u4F3A\u4F3C\u4F7F\u4FDF\u50FF\u53F2\u53F8\u5506" +
                "\u55E3\u56DB\u58EB\u5962\u5A11\u5BEB\u5BFA\u5C04" +
                "\u5DF3\u5E2B\u5F99\u601D\u6368\u659C\u65AF\u67F6" +
                "\u67FB\u68AD\u6B7B\u6C99\u6CD7\u6E23\u7009\u7345" +
                "\u7802\u793E\u7940\u7960\u79C1\u7BE9\u7D17\u7D72" +
                "\u8086\u820D\u838E\u84D1\u86C7\u88DF\u8A50\u8A5E" +
                "\u8B1D\u8CDC\u8D66\u8FAD\u90AA\u98FC\u99DF\u9E9D" +
                "\u524A\uF969\u6714\uF96A\u5098\u522A\u5C71\u6563" +
                "\u6C55\u73CA\u7523\u759D\u7B97\u849C\u9178\u9730" +
                "\u4E77\u6492\u6BBA\u715E\u85A9\u4E09\uF96B\u6749" +
                "\u68EE\u6E17\u829F\u8518\u886B\u63F7\u6F81\u9212" +
                "\u98AF\u4E0A\u50B7\u50CF\u511F\u5546\u55AA\u5617" +
                "\u5B40\u5C19\u5CE0\u5E38\u5E8A\u5EA0\u5EC2\u60F3" +
                "\u6851\u6A61\u6E58\u723D\u7240\u72C0\u76F8\u7965" +
                "\u7BB1\u7FD4\u88F3\u89F4\u8A73\u8C61\u8CDE\u971C" +
                "\u585E\u74BD\u8CFD\u55C7\uF96C\u7A61\u7D22\u8272" +
                "\u7272\u751F\u7525\uF96D\u7B19\u5885\u58FB\u5DBC" +
                "\u5E8F\u5EB6\u5F90\u6055\u6292\u637F\u654D\u6691" +
                "\u66D9\u66F8\u6816\u68F2\u7280\u745E\u7B6E\u7D6E" +
                "\u7DD6\u7F72\u80E5\u8212\u85AF\u897F\u8A93\u901D" +
                "\u92E4\u9ECD\u9F20\u5915\u596D\u5E2D\u60DC\u6614" +
                "\u6673\u6790\u6C50\u6DC5\u6F5F\u77F3\u78A9\u84C6" +
                "\u91CB\u932B\u4ED9\u50CA\u5148\u5584\u5B0B\u5BA3" +
                "\u6247\u657E\u65CB\u6E32\u717D\u7401\u7444\u7487" +
                "\u74BF\u766C\u79AA\u7DDA\u7E55\u7FA8\u817A\u81B3" +
                "\u8239\u861A\u87EC\u8A75\u8DE3\u9078\u9291\u9425" +
                "\u994D\u9BAE\u5368\u5C51\u6954\u6CC4\u6D29\u6E2B" +
                "\u820C\u859B\u893B\u8A2D\u8AAA\u96EA\u9F67\u5261" +
                "\u66B9\u6BB2\u7E96\u87FE\u8D0D\u9583\u965D\u651D" +
                "\u6D89\u71EE\uF96E\u57CE\u59D3\u5BAC\u6027\u60FA" +
                "\u6210\u661F\u665F\u7329\u73F9\u76DB\u7701\u7B6C" +
                "\u8056\u8072\u8165\u8AA0\u9192\u4E16\u52E2\u6B72" +
                "\u6D17\u7A05\u7B39\u7D30\uF96F\u8CB0\u53EC\u562F" +
                "\u5851\u5BB5\u5C0F\u5C11\u5DE2\u6240\u6383\u6414" +
                "\u662D\u68B3\u6CBC\u6D88\u6EAF\u701F\u70A4\u71D2" +
                "\u7526\u758F\u758E\u7619\u7B11\u7BE0\u7C2B\u7D20" +
                "\u7D39\u852C\u856D\u8607\u8A34\u900D\u9061\u90B5" +
                "\u92B7\u97F6\u9A37\u4FD7\u5C6C\u675F\u6D91\u7C9F" +
                "\u7E8C\u8B16\u8D16\u901F\u5B6B\u5DFD\u640D\u84C0" +
                "\u905C\u98E1\u7387\u5B8B\u609A\u677E\u6DDE\u8A1F" +
                "\u8AA6\u9001\u980C\u5237\uF970\u7051\u788E\u9396" +
                "\u8870\u91D7\u4FEE\u53D7\u55FD\u56DA\u5782\u58FD" +
                "\u5AC2\u5B88\u5CAB\u5CC0\u5E25\u6101\u620D\u624B" +
                "\u6388\u641C\u6536\u6578\u6A39\u6B8A\u6C34\u6D19" +
                "\u6F31\u71E7\u72E9\u7378\u7407\u74B2\u7626\u7761" +
                "\u79C0\u7A57\u7AEA\u7CB9\u7D8F\u7DAC\u7E61\u7F9E" +
                "\u8129\u8331\u8490\u84DA\u85EA\u8896\u8AB0\u8B90" +
                "\u8F38\u9042\u9083\u916C\u9296\u92B9\u968B\u96A7" +
                "\u96A8\u96D6\u9700\u9808\u9996\u9AD3\u9B1A\u53D4" +
                "\u587E\u5919\u5B70\u5BBF\u6DD1\u6F5A\u719F\u7421" +
                "\u74B9\u8085\u83FD\u5DE1\u5F87\u5FAA\u6042\u65EC" +
                "\u6812\u696F\u6A53\u6B89\u6D35\u6DF3\u73E3\u76FE" +
                "\u77AC\u7B4D\u7D14\u8123\u821C\u8340\u84F4\u8563" +
                "\u8A62\u8AC4\u9187\u931E\u9806\u99B4\u620C\u8853" +
                "\u8FF0\u9265\u5D07\u5D27\u5D69\u745F\u819D\u8768" +
                "\u6FD5\u62FE\u7FD2\u8936\u8972\u4E1E\u4E58\u50E7" +
                "\u52DD\u5347\u627F\u6607\u7E69\u8805\u965E\u4F8D" +
                "\u5319\u5636\u59CB\u5AA4\u5C38\u5C4E\u5C4D\u5E02" +
                "\u5F11\u6043\u65BD\u662F\u6642\u67BE\u67F4\u731C" +
                "\u77E2\u793A\u7FC5\u8494\u84CD\u8996\u8A66\u8A69" +
                "\u8AE1\u8C55\u8C7A\u57F4\u5BD4\u5F0F\u606F\u62ED" +
                "\u690D\u6B96\u6E5C\u7184\u7BD2\u8755\u8B58\u8EFE" +
                "\u98DF\u98FE\u4F38\u4F81\u4FE1\u547B\u5A20\u5BB8" +
                "\u613C\u65B0\u6668\u71FC\u7533\u795E\u7D33\u814E" +
                "\u81E3\u8398\u85AA\u85CE\u8703\u8A0A\u8EAB\u8F9B" +
                "\uF971\u8FC5\u5931\u5BA4\u5BE6\u6089\u5BE9\u5C0B" +
                "\u5FC3\u6C81\uF972\u6DF1\u700B\u751A\u82AF\u8AF6" +
                "\u4EC0\u5341\uF973\u96D9\u6C0F\u4E9E\u4FC4\u5152" +
                "\u555E\u5A25\u5CE8\u6211\u7259\u82BD\u83AA\u86FE" +
                "\u8859\u8A1D\u963F\u96C5\u9913\u9D09\u9D5D\u580A" +
                "\u5CB3\u5DBD\u5E44\u60E1\u6115\u63E1\u6A02\u6E25" +
                "\u9102\u9354\u984E\u9C10\u9F77\u5B89\u5CB8\u6309" +
                "\u664F\u6848\u773C\u96C1\u978D\u9854\u9B9F\u65A1" +
                "\u8B01\u8ECB\u95BC\u5535\u5CA9\u5DD6\u5EB5\u6697" +
                "\u764C\u83F4\u95C7\u58D3\u62BC\u72CE\u9D28\u4EF0" +
                "\u592E\u600F\u663B\u6B83\u79E7\u9D26\u5393\u54C0" +
                "\u57C3\u5D16\u611B\u66D6\u6DAF\u788D\u827E\u9698" +
                "\u9744\u5384\u627C\u6396\u6DB2\u7E0A\u814B\u984D" +
                "\u6AFB\u7F4C\u9DAF\u9E1A\u4E5F\u503B\u51B6\u591C" +
                "\u60F9\u63F6\u6930\u723A\u8036\uF974\u91CE\u5F31" +
                "\uF975\uF976\u7D04\u82E5\u846F\u84BB\u85E5\u8E8D" +
                "\uF977\u4F6F\uF978\uF979\u58E4\u5B43\u6059\u63DA" +
                "\u6518\u656D\u6698\uF97A\u694A\u6A23\u6D0B\u7001" +
                "\u716C\u75D2\u760D\u79B3\u7A70\uF97B\u7F8A\uF97C" +
                "\u8944\uF97D\u8B93\u91C0\u967D\uF97E\u990A\u5704" +
                "\u5FA1\u65BC\u6F01\u7600\u79A6\u8A9E\u99AD\u9B5A" +
                "\u9F6C\u5104\u61B6\u6291\u6A8D\u81C6\u5043\u5830" +
                "\u5F66\u7109\u8A00\u8AFA\u5B7C\u8616\u4FFA\u513C" +
                "\u56B4\u5944\u63A9\u6DF9\u5DAA\u696D\u5186\u4E88" +
                "\u4F59\uF97F\uF980\uF981\u5982\uF982\uF983\u6B5F" +
                "\u6C5D\uF984\u74B5\u7916\uF985\u8207\u8245\u8339" +
                "\u8F3F\u8F5D\uF986\u9918\uF987\uF988\uF989\u4EA6" +
                "\uF98A\u57DF\u5F79\u6613\uF98B\uF98C\u75AB\u7E79" +
                "\u8B6F\uF98D\u9006\u9A5B\u56A5\u5827\u59F8\u5A1F" +
                "\u5BB4\uF98E\u5EF6\uF98F\uF990\u6350\u633B\uF991" +
                "\u693D\u6C87\u6CBF\u6D8E\u6D93\u6DF5\u6F14\uF992" +
                "\u70DF\u7136\u7159\uF993\u71C3\u71D5\uF994\u784F" +
                "\u786F\uF995\u7B75\u7DE3\uF996\u7E2F\uF997\u884D" +
                "\u8EDF\uF998\uF999\uF99A\u925B\uF99B\u9CF6\uF99C" +
                "\uF99D\uF99E\u6085\u6D85\uF99F\u71B1\uF9A0\uF9A1" +
                "\u95B1\u53AD\uF9A2\uF9A3\uF9A4\u67D3\uF9A5\u708E" +
                "\u7130\u7430\u8276\u82D2\uF9A6\u95BB\u9AE5\u9E7D" +
                "\u66C4\uF9A7\u71C1\u8449\uF9A8\uF9A9\u584B\uF9AA" +
                "\uF9AB\u5DB8\u5F71\uF9AC\u6620\u668E\u6979\u69AE" +
                "\u6C38\u6CF3\u6E36\u6F41\u6FDA\u701B\u702F\u7150" +
                "\u71DF\u7370\uF9AD\u745B\uF9AE\u74D4\u76C8\u7A4E" +
                "\u7E93\uF9AF\uF9B0\u82F1\u8A60\u8FCE\uF9B1\u9348" +
                "\uF9B2\u9719\uF9B3\uF9B4\u4E42\u502A\uF9B5\u5208" +
                "\u53E1\u66F3\u6C6D\u6FCA\u730A\u777F\u7A62\u82AE" +
                "\u85DD\u8602\uF9B6\u88D4\u8A63\u8B7D\u8C6B\uF9B7" +
                "\u92B3\uF9B8\u9713\u9810\u4E94\u4F0D\u4FC9\u50B2" +
                "\u5348\u543E\u5433\u55DA\u5862\u58BA\u5967\u5A1B" +
                "\u5BE4\u609F\uF9B9\u61CA\u6556\u65FF\u6664\u68A7" +
                "\u6C5A\u6FB3\u70CF\u71AC\u7352\u7B7D\u8708\u8AA4" +
                "\u9C32\u9F07\u5C4B\u6C83\u7344\u7389\u923A\u6EAB" +
                "\u7465\u761F\u7A69\u7E15\u860A\u5140\u58C5\u64C1" +
                "\u74EE\u7515\u7670\u7FC1\u9095\u96CD\u9954\u6E26" +
                "\u74E6\u7AA9\u7AAA\u81E5\u86D9\u8778\u8A1B\u5A49" +
                "\u5B8C\u5B9B\u68A1\u6900\u6D63\u73A9\u7413\u742C" +
                "\u7897\u7DE9\u7FEB\u8118\u8155\u839E\u8C4C\u962E" +
                "\u9811\u66F0\u5F80\u65FA\u6789\u6C6A\u738B\u502D" +
                "\u5A03\u6B6A\u77EE\u5916\u5D6C\u5DCD\u7325\u754F" +
                "\uF9BA\uF9BB\u50E5\u51F9\u582F\u592D\u5996\u59DA" +
                "\u5BE5\uF9BC\uF9BD\u5DA2\u62D7\u6416\u6493\u64FE" +
                "\uF9BE\u66DC\uF9BF\u6A48\uF9C0\u71FF\u7464\uF9C1" +
                "\u7A88\u7AAF\u7E47\u7E5E\u8000\u8170\uF9C2\u87EF" +
                "\u8981\u8B20\u9059\uF9C3\u9080\u9952\u617E\u6B32" +
                "\u6D74\u7E1F\u8925\u8FB1\u4FD1\u50AD\u5197\u52C7" +
                "\u57C7\u5889\u5BB9\u5EB8\u6142\u6995\u6D8C\u6E67" +
                "\u6EB6\u7194\u7462\u7528\u752C\u8073\u8338\u84C9" +
                "\u8E0A\u9394\u93DE\uF9C4\u4E8E\u4F51\u5076\u512A" +
                "\u53C8\u53CB\u53F3\u5B87\u5BD3\u5C24\u611A\u6182" +
                "\u65F4\u725B\u7397\u7440\u76C2\u7950\u7991\u79B9" +
                "\u7D06\u7FBD\u828B\u85D5\u865E\u8FC2\u9047\u90F5" +
                "\u91EA\u9685\u96E8\u96E9\u52D6\u5F67\u65ED\u6631" +
                "\u682F\u715C\u7A36\u90C1\u980A\u4E91\uF9C5\u6A52" +
                "\u6B9E\u6F90\u7189\u8018\u82B8\u8553\u904B\u9695" +
                "\u96F2\u97FB\u851A\u9B31\u4E90\u718A\u96C4\u5143" +
                "\u539F\u54E1\u5713\u5712\u57A3\u5A9B\u5AC4\u5BC3" +
                "\u6028\u613F\u63F4\u6C85\u6D39\u6E72\u6E90\u7230" +
                "\u733F\u7457\u82D1\u8881\u8F45\u9060\uF9C6\u9662" +
                "\u9858\u9D1B\u6708\u8D8A\u925E\u4F4D\u5049\u50DE" +
                "\u5371\u570D\u59D4\u5A01\u5C09\u6170\u6690\u6E2D" +
                "\u7232\u744B\u7DEF\u80C3\u840E\u8466\u853F\u875F" +
                "\u885B\u8918\u8B02\u9055\u97CB\u9B4F\u4E73\u4F91" +
                "\u5112\u516A\uF9C7\u552F\u55A9\u5B7A\u5BA5\u5E7C" +
                "\u5E7D\u5EBE\u60A0\u60DF\u6108\u6109\u63C4\u6538" +
                "\u6709\uF9C8\u67D4\u67DA\uF9C9\u6961\u6962\u6CB9" +
                "\u6D27\uF9CA\u6E38\uF9CB\u6FE1\u7336\u7337\uF9CC" +
                "\u745C\u7531\uF9CD\u7652\uF9CE\uF9CF\u7DAD\u81FE" +
                "\u8438\u88D5\u8A98\u8ADB\u8AED\u8E30\u8E42\u904A" +
                "\u903E\u907A\u9149\u91C9\u936E\uF9D0\uF9D1\u5809" +
                "\uF9D2\u6BD3\u8089\u80B2\uF9D3\uF9D4\u5141\u596B" +
                "\u5C39\uF9D5\uF9D6\u6F64\u73A7\u80E4\u8D07\uF9D7" +
                "\u9217\u958F\uF9D8\uF9D9\uF9DA\uF9DB\u807F\u620E" +
                "\u701C\u7D68\u878D\uF9DC\u57A0\u6069\u6147\u6BB7" +
                "\u8ABE\u9280\u96B1\u4E59\u541F\u6DEB\u852D\u9670" +
                "\u97F3\u98EE\u63D6\u6CE3\u9091\u51DD\u61C9\u81BA" +
                "\u9DF9\u4F9D\u501A\u5100\u5B9C\u610F\u61FF\u64EC" +
                "\u6905\u6BC5\u7591\u77E3\u7FA9\u8264\u858F\u87FB" +
                "\u8863\u8ABC\u8B70\u91AB\u4E8C\u4EE5\u4F0A\uF9DD" +
                "\uF9DE\u5937\u59E8\uF9DF\u5DF2\u5F1B\u5F5B\u6021" +
                "\uF9E0\uF9E1\uF9E2\uF9E3\u723E\u73E5\uF9E4\u7570" +
                "\u75CD\uF9E5\u79FB\uF9E6\u800C\u8033\u8084\u82E1" +
                "\u8351\uF9E7\uF9E8\u8CBD\u8CB3\u9087\uF9E9\uF9EA" +
                "\u98F4\u990C\uF9EB\uF9EC\u7037\u76CA\u7FCA\u7FCC" +
                "\u7FFC\u8B1A\u4EBA\u4EC1\u5203\u5370\uF9ED\u54BD" +
                "\u56E0\u59FB\u5BC5\u5F15\u5FCD\u6E6E\uF9EE\uF9EF" +
                "\u7D6A\u8335\uF9F0\u8693\u8A8D\uF9F1\u976D\u9777" +
                "\uF9F2\uF9F3\u4E00\u4F5A\u4F7E\u58F9\u65E5\u6EA2" +
                "\u9038\u93B0\u99B9\u4EFB\u58EC\u598A\u59D9\u6041" +
                "\uF9F4\uF9F5\u7A14\uF9F6\u834F\u8CC3\u5165\u5344" +
                "\uF9F7\uF9F8\uF9F9\u4ECD\u5269\u5B55\u82BF\u4ED4" +
                "\u523A\u54A8\u59C9\u59FF\u5B50\u5B57\u5B5C\u6063" +
                "\u6148\u6ECB\u7099\u716E\u7386\u74F7\u75B5\u78C1" +
                "\u7D2B\u8005\u81EA\u8328\u8517\u85C9\u8AEE\u8CC7" +
                "\u96CC\u4F5C\u52FA\u56BC\u65AB\u6628\u707C\u70B8" +
                "\u7235\u7DBD\u828D\u914C\u96C0\u9D72\u5B71\u68E7" +
                "\u6B98\u6F7A\u76DE\u5C91\u66AB\u6F5B\u7BB4\u7C2A" +
                "\u8836\u96DC\u4E08\u4ED7\u5320\u5834\u58BB\u58EF" +
                "\u596C\u5C07\u5E33\u5E84\u5F35\u638C\u66B2\u6756" +
                "\u6A1F\u6AA3\u6B0C\u6F3F\u7246\uF9FA\u7350\u748B" +
                "\u7AE0\u7CA7\u8178\u81DF\u81E7\u838A\u846C\u8523" +
                "\u8594\u85CF\u88DD\u8D13\u91AC\u9577\u969C\u518D" +
                "\u54C9\u5728\u5BB0\u624D\u6750\u683D\u6893\u6E3D" +
                "\u6ED3\u707D\u7E21\u88C1\u8CA1\u8F09\u9F4B\u9F4E" +
                "\u722D\u7B8F\u8ACD\u931A\u4F47\u4F4E\u5132\u5480" +
                "\u59D0\u5E95\u62B5\u6775\u696E\u6A17\u6CAE\u6E1A" +
                "\u72D9\u732A\u75BD\u7BB8\u7D35\u82E7\u83F9\u8457" +
                "\u85F7\u8A5B\u8CAF\u8E87\u9019\u90B8\u96CE\u9F5F" +
                "\u52E3\u540A\u5AE1\u5BC2\u6458\u6575\u6EF4\u72C4" +
                "\uF9FB\u7684\u7A4D\u7B1B\u7C4D\u7E3E\u7FDF\u837B" +
                "\u8B2B\u8CCA\u8D64\u8DE1\u8E5F\u8FEA\u8FF9\u9069" +
                "\u93D1\u4F43\u4F7A\u50B3\u5168\u5178\u524D\u526A" +
                "\u5861\u587C\u5960\u5C08\u5C55\u5EDB\u609B\u6230" +
                "\u6813\u6BBF\u6C08\u6FB1\u714E\u7420\u7530\u7538" +
                "\u7551\u7672\u7B4C\u7B8B\u7BAD\u7BC6\u7E8F\u8A6E" +
                "\u8F3E\u8F49\u923F\u9293\u9322\u942B\u96FB\u985A" +
                "\u986B\u991E\u5207\u622A\u6298\u6D59\u7664\u7ACA" +
                "\u7BC0\u7D76\u5360\u5CBE\u5E97\u6F38\u70B9\u7C98" +
                "\u9711\u9B8E\u9EDE\u63A5\u647A\u8776\u4E01\u4E95" +
                "\u4EAD\u505C\u5075\u5448\u59C3\u5B9A\u5E40\u5EAD" +
                "\u5EF7\u5F81\u60C5\u633A\u653F\u6574\u65CC\u6676" +
                "\u6678\u67FE\u6968\u6A89\u6B63\u6C40\u6DC0\u6DE8" +
                "\u6E1F\u6E5E\u701E\u70A1\u738E\u73FD\u753A\u775B" +
                "\u7887\u798E\u7A0B\u7A7D\u7CBE\u7D8E\u8247\u8A02" +
                "\u8AEA\u8C9E\u912D\u914A\u91D8\u9266\u92CC\u9320" +
                "\u9706\u9756\u975C\u9802\u9F0E\u5236\u5291\u557C" +
                "\u5824\u5E1D\u5F1F\u608C\u63D0\u68AF\u6FDF\u796D" +
                "\u7B2C\u81CD\u85BA\u88FD\u8AF8\u8E44\u918D\u9664" +
                "\u969B\u973D\u984C\u9F4A\u4FCE\u5146\u51CB\u52A9" +
                "\u5632\u5F14\u5F6B\u63AA\u64CD\u65E9\u6641\u66FA" +
                "\u66F9\u671D\u689D\u68D7\u69FD\u6F15\u6F6E\u7167" +
                "\u71E5\u722A\u74AA\u773A\u7956\u795A\u79DF\u7A20" +
                "\u7A95\u7C97\u7CDF\u7D44\u7E70\u8087\u85FB\u86A4" +
                "\u8A54\u8ABF\u8D99\u8E81\u9020\u906D\u91E3\u963B" +
                "\u96D5\u9CE5\u65CF\u7C07\u8DB3\u93C3\u5B58\u5C0A" +
                "\u5352\u62D9\u731D\u5027\u5B97\u5F9E\u60B0\u616B" +
                "\u68D5\u6DD9\u742E\u7A2E\u7D42\u7D9C\u7E31\u816B" +
                "\u8E2A\u8E35\u937E\u9418\u4F50\u5750\u5DE6\u5EA7" +
                "\u632B\u7F6A\u4E3B\u4F4F\u4F8F\u505A\u59DD\u80C4" +
                "\u546A\u5468\u55FE\u594F\u5B99\u5DDE\u5EDA\u665D" +
                "\u6731\u67F1\u682A\u6CE8\u6D32\u6E4A\u6F8D\u70B7" +
                "\u73E0\u7587\u7C4C\u7D02\u7D2C\u7DA2\u821F\u86DB" +
                "\u8A3B\u8A85\u8D70\u8E8A\u8F33\u9031\u914E\u9152" +
                "\u9444\u99D0\u7AF9\u7CA5\u4FCA\u5101\u51C6\u57C8" +
                "\u5BEF\u5CFB\u6659\u6A3D\u6D5A\u6E96\u6FEC\u710C" +
                "\u756F\u7AE3\u8822\u9021\u9075\u96CB\u99FF\u8301" +
                "\u4E2D\u4EF2\u8846\u91CD\u537D\u6ADB\u696B\u6C41" +
                "\u847A\u589E\u618E\u66FE\u62EF\u70DD\u7511\u75C7" +
                "\u7E52\u84B8\u8B49\u8D08\u4E4B\u53EA\u54AB\u5730" +
                "\u5740\u5FD7\u6301\u6307\u646F\u652F\u65E8\u667A" +
                "\u679D\u67B3\u6B62\u6C60\u6C9A\u6F2C\u77E5\u7825" +
                "\u7949\u7957\u7D19\u80A2\u8102\u81F3\u829D\u82B7" +
                "\u8718\u8A8C\uF9FC\u8D04\u8DBE\u9072\u76F4\u7A19" +
                "\u7A37\u7E54\u8077\u5507\u55D4\u5875\u632F\u6422" +
                "\u6649\u664B\u686D\u699B\u6B84\u6D25\u6EB1\u73CD" +
                "\u7468\u74A1\u755B\u75B9\u76E1\u771E\u778B\u79E6" +
                "\u7E09\u7E1D\u81FB\u852F\u8897\u8A3A\u8CD1\u8EEB" +
                "\u8FB0\u9032\u93AD\u9663\u9673\u9707\u4F84\u53F1" +
                "\u59EA\u5AC9\u5E19\u684E\u74C6\u75BE\u79E9\u7A92" +
                "\u81A3\u86ED\u8CEA\u8DCC\u8FED\u659F\u6715\uF9FD" +
                "\u57F7\u6F57\u7DDD\u8F2F\u93F6\u96C6\u5FB5\u61F2" +
                "\u6F84\u4E14\u4F98\u501F\u53C9\u55DF\u5D6F\u5DEE" +
                "\u6B21\u6B64\u78CB\u7B9A\uF9FE\u8E49\u8ECA\u906E" +
                "\u6349\u643E\u7740\u7A84\u932F\u947F\u9F6A\u64B0" +
                "\u6FAF\u71E6\u74A8\u74DA\u7AC4\u7C12\u7E82\u7CB2" +
                "\u7E98\u8B9A\u8D0A\u947D\u9910\u994C\u5239\u5BDF" +
                "\u64E6\u672D\u7D2E\u50ED\u53C3\u5879\u6158\u6159" +
                "\u61FA\u65AC\u7AD9\u8B92\u8B96\u5009\u5021\u5275" +
                "\u5531\u5A3C\u5EE0\u5F70\u6134\u655E\u660C\u6636" +
                "\u66A2\u69CD\u6EC4\u6F32\u7316\u7621\u7A93\u8139" +
                "\u8259\u83D6\u84BC\u50B5\u57F0\u5BC0\u5BE8\u5F69" +
                "\u63A1\u7826\u7DB5\u83DC\u8521\u91C7\u91F5\u518A" +
                "\u67F5\u7B56\u8CAC\u51C4\u59BB\u60BD\u8655\u501C" +
                "\uF9FF\u5254\u5C3A\u617D\u621A\u62D3\u64F2\u65A5" +
                "\u6ECC\u7620\u810A\u8E60\u965F\u96BB\u4EDF\u5343" +
                "\u5598\u5929\u5DDD\u64C5\u6CC9\u6DFA\u7394\u7A7F" +
                "\u821B\u85A6\u8CE4\u8E10\u9077\u91E7\u95E1\u9621" +
                "\u97C6\u51F8\u54F2\u5586\u5FB9\u64A4\u6F88\u7DB4" +
                "\u8F1F\u8F4D\u9435\u50C9\u5C16\u6CBE\u6DFB\u751B" +
                "\u77BB\u7C3D\u7C64\u8A79\u8AC2\u581E\u59BE\u5E16" +
                "\u6377\u7252\u758A\u776B\u8ADC\u8CBC\u8F12\u5EF3" +
                "\u6674\u6DF8\u807D\u83C1\u8ACB\u9751\u9BD6\uFA00" +
                "\u5243\u66FF\u6D95\u6EEF\u7DE0\u8AE6\u902E\u905E" +
                "\u9AD4\u521D\u527F\u54E8\u6194\u6284\u62DB\u68A2" +
                "\u6912\u695A\u6A35\u7092\u7126\u785D\u7901\u790E" +
                "\u79D2\u7A0D\u8096\u8278\u82D5\u8349\u8549\u8C82" +
                "\u8D85\u9162\u918B\u91AE\u4FC3\u56D1\u71ED\u77D7" +
                "\u8700\u89F8\u5BF8\u5FD6\u6751\u90A8\u53E2\u585A" +
                "\u5BF5\u60A4\u6181\u6460\u7E3D\u8070\u8525\u9283" +
                "\u64AE\u50AC\u5D14\u6700\u589C\u62BD\u63A8\u690E" +
                "\u6978\u6A1E\u6E6B\u76BA\u79CB\u82BB\u8429\u8ACF" +
                "\u8DA8\u8FFD\u9112\u914B\u919C\u9310\u9318\u939A" +
                "\u96DB\u9A36\u9C0D\u4E11\u755C\u795D\u7AFA\u7B51" +
                "\u7BC9\u7E2E\u84C4\u8E59\u8E74\u8EF8\u9010\u6625" +
                "\u693F\u7443\u51FA\u672E\u9EDC\u5145\u5FE0\u6C96" +
                "\u87F2\u885D\u8877\u60B4\u81B5\u8403\u8D05\u53D6" +
                "\u5439\u5634\u5A36\u5C31\u708A\u7FE0\u805A\u8106" +
                "\u81ED\u8DA3\u9189\u9A5F\u9DF2\u5074\u4EC4\u53A0" +
                "\u60FB\u6E2C\u5C64\u4F88\u5024\u55E4\u5CD9\u5E5F" +
                "\u6065\u6894\u6CBB\u6DC4\u71BE\u75D4\u75F4\u7661" +
                "\u7A1A\u7A49\u7DC7\u7DFB\u7F6E\u81F4\u86A9\u8F1C" +
                "\u96C9\u99B3\u9F52\u5247\u52C5\u98ED\u89AA\u4E03" +
                "\u67D2\u6F06\u4FB5\u5BE2\u6795\u6C88\u6D78\u741B" +
                "\u7827\u91DD\u937C\u87C4\u79E4\u7A31\u5FEB\u4ED6" +
                "\u54A4\u553E\u58AE\u59A5\u60F0\u6253\u62D6\u6736" +
                "\u6955\u8235\u9640\u99B1\u99DD\u502C\u5353\u5544" +
                "\u577C\uFA01\u6258\uFA02\u64E2\u666B\u67DD\u6FC1" +
                "\u6FEF\u7422\u7438\u8A17\u9438\u5451\u5606\u5766" +
                "\u5F48\u619A\u6B4E\u7058\u70AD\u7DBB\u8A95\u596A" +
                "\u812B\u63A2\u7708\u803D\u8CAA\u5854\u642D\u69BB" +
                "\u5B95\u5E11\u6E6F\uFA03\u8569\u514C\u53F0\u592A" +
                "\u6020\u614B\u6B86\u6C70\u6CF0\u7B1E\u80CE\u82D4" +
                "\u8DC6\u90B0\u98B1\uFA04\u64C7\u6FA4\u6491\u6504" +
                "\u514E\u5410\u571F\u8A0E\u615F\u6876\uFA05\u75DB" +
                "\u7B52\u7D71\u901A\u5806\u69CC\u817F\u892A\u9000" +
                "\u9839\u5078\u5957\u59AC\u6295\u900F\u9B2A\u615D" +
                "\u7279\u95D6\u5761\u5A46\u5DF4\u628A\u64AD\u64FA" +
                "\u6777\u6CE2\u6D3E\u722C\u7436\u7834\u7F77\u82AD" +
                "\u8DDB\u9817\u5224\u5742\u677F\u7248\u74E3\u8CA9" +
                "\u8FA6\u9211\u962A\u516B\u53ED\u634C\u4F69\u5504" +
                "\u6096\u6557\u6C9B\u6D7F\u724C\u72FD\u7A17\u8987" +
                "\u8C9D\u5F6D\u6F8E\u70F9\u81A8\u610E\u4FBF\u504F" +
                "\u6241\u7247\u7BC7\u7DE8\u7FE9\u904D\u97AD\u9A19" +
                "\u8CB6\u576A\u5E73\u67B0\u840D\u8A55\u5420\u5B16" +
                "\u5E63\u5EE2\u5F0A\u6583\u80BA\u853D\u9589\u965B" +
                "\u4F48\u5305\u530D\u530F\u5486\u54FA\u5703\u5E03" +
                "\u6016\u629B\u62B1\u6355\uFA06\u6CE1\u6D66\u75B1" +
                "\u7832\u80DE\u812F\u82DE\u8461\u84B2\u888D\u8912" +
                "\u900B\u92EA\u98FD\u9B91\u5E45\u66B4\u66DD\u7011" +
                "\u7206\uFA07\u4FF5\u527D\u5F6A\u6153\u6753\u6A19" +
                "\u6F02\u74E2\u7968\u8868\u8C79\u98C7\u98C4\u9A43" +
                "\u54C1\u7A1F\u6953\u8AF7\u8C4A\u98A8\u99AE\u5F7C" +
                "\u62AB\u75B2\u76AE\u88AB\u907F\u9642\u5339\u5F3C" +
                "\u5FC5\u6CCC\u73CC\u7562\u758B\u7B46\u82FE\u999D" +
                "\u4E4F\u903C\u4E0B\u4F55\u53A6\u590F\u5EC8\u6630" +
                "\u6CB3\u7455\u8377\u8766\u8CC0\u9050\u971E\u9C15" +
                "\u58D1\u5B78\u8650\u8B14\u9DB4\u5BD2\u6068\u608D" +
                "\u65F1\u6C57\u6F22\u6FA3\u701A\u7F55\u7FF0\u9591" +
                "\u9592\u9650\u97D3\u5272\u8F44\u51FD\u542B\u54B8" +
                "\u5563\u558A\u6ABB\u6DB5\u7DD8\u8266\u929C\u9677" +
                "\u9E79\u5408\u54C8\u76D2\u86E4\u95A4\u95D4\u965C" +
                "\u4EA2\u4F09\u59EE\u5AE6\u5DF7\u6052\u6297\u676D" +
                "\u6841\u6C86\u6E2F\u7F38\u809B\u822A\uFA08\uFA09" +
                "\u9805\u4EA5\u5055\u54B3\u5793\u595A\u5B69\u5BB3" +
                "\u61C8\u6977\u6D77\u7023\u87F9\u89E3\u8A72\u8AE7" +
                "\u9082\u99ED\u9AB8\u52BE\u6838\u5016\u5E78\u674F" +
                "\u8347\u884C\u4EAB\u5411\u56AE\u73E6\u9115\u97FF" +
                "\u9909\u9957\u9999\u5653\u589F\u865B\u8A31\u61B2" +
                "\u6AF6\u737B\u8ED2\u6B47\u96AA\u9A57\u5955\u7200" +
                "\u8D6B\u9769\u4FD4\u5CF4\u5F26\u61F8\u665B\u6CEB" +
                "\u70AB\u7384\u73B9\u73FE\u7729\u774D\u7D43\u7D62" +
                "\u7E23\u8237\u8852\uFA0A\u8CE2\u9249\u986F\u5B51" +
                "\u7A74\u8840\u9801\u5ACC\u4FE0\u5354\u593E\u5CFD" +
                "\u633E\u6D79\u72F9\u8105\u8107\u83A2\u92CF\u9830" +
                "\u4EA8\u5144\u5211\u578B\u5F62\u6CC2\u6ECE\u7005" +
                "\u7050\u70AF\u7192\u73E9\u7469\u834A\u87A2\u8861" +
                "\u9008\u90A2\u93A3\u99A8\u516E\u5F57\u60E0\u6167" +
                "\u66B3\u8559\u8E4A\u91AF\u978B\u4E4E\u4E92\u547C" +
                "\u58D5\u58FA\u597D\u5CB5\u5F27\u6236\u6248\u660A" +
                "\u6667\u6BEB\u6D69\u6DCF\u6E56\u6EF8\u6F94\u6FE0" +
                "\u6FE9\u705D\u72D0\u7425\u745A\u74E0\u7693\u795C" +
                "\u7CCA\u7E1E\u80E1\u82A6\u846B\u84BF\u864E\u865F" +
                "\u8774\u8B77\u8C6A\u93AC\u9800\u9865\u60D1\u6216" +
                "\u9177\u5A5A\u660F\u6DF7\u6E3E\u743F\u9B42\u5FFD" +
                "\u60DA\u7B0F\u54C4\u5F18\u6C5E\u6CD3\u6D2A\u70D8" +
                "\u7D05\u8679\u8A0C\u9D3B\u5316\u548C\u5B05\u6A3A" +
                "\u706B\u7575\u798D\u79BE\u82B1\u83EF\u8A71\u8B41" +
                "\u8CA8\u9774\uFA0B\u64F4\u652B\u78BA\u78BB\u7A6B" +
                "\u4E38\u559A\u5950\u5BA6\u5E7B\u60A3\u63DB\u6B61" +
                "\u6665\u6853\u6E19\u7165\u74B0\u7D08\u9084\u9A69" +
                "\u9C25\u6D3B\u6ED1\u733E\u8C41\u95CA\u51F0\u5E4C" +
                "\u5FA8\u604D\u60F6\u6130\u614C\u6643\u6644\u69A5" +
                "\u6CC1\u6E5F\u6EC9\u6F62\u714C\u749C\u7687\u7BC1" +
                "\u7C27\u8352\u8757\u9051\u968D\u9EC3\u532F\u56DE" +
                "\u5EFB\u5F8A\u6062\u6094\u61F7\u6666\u6703\u6A9C" +
                "\u6DEE\u6FAE\u7070\u736A\u7E6A\u81BE\u8334\u86D4" +
                "\u8AA8\u8CC4\u5283\u7372\u5B96\u6A6B\u9404\u54EE" +
                "\u5686\u5B5D\u6548\u6585\u66C9\u689F\u6D8D\u6DC6" +
                "\u723B\u80B4\u9175\u9A4D\u4FAF\u5019\u539A\u540E" +
                "\u543C\u5589\u55C5\u5E3F\u5F8C\u673D\u7166\u73DD" +
                "\u9005\u52DB\u52F3\u5864\u58CE\u7104\u718F\u71FB" +
                "\u85B0\u8A13\u6688\u85A8\u55A7\u6684\u714A\u8431" +
                "\u5349\u5599\u6BC1\u5F59\u5FBD\u63EE\u6689\u7147" +
                "\u8AF1\u8F1D\u9EBE\u4F11\u643A\u70CB\u7566\u8667" +
                "\u6064\u8B4E\u9DF8\u5147\u51F6\u5308\u6D36\u80F8" +
                "\u9ED1\u6615\u6B23\u7098\u75D5\u5403\u5C79\u7D07" +
                "\u8A16\u6B20\u6B3D\u6B46\u5438\u6070\u6D3D\u7FD5" +
                "\u8208\u50D6\u51DE\u559C\u566B\u56CD\u59EC\u5B09" +
                "\u5E0C\u6199\u6198\u6231\u665E\u66E6\u7199\u71B9" +
                "\u71BA\u72A7\u79A7\u7A00\u7FB2\u8A70\uE05E\uE05F" +
                "\uE060\uE061\uE062\uE063\uE064\uE065\uE066\uE067" +
                "\uE068\uE069\uE06A\uE06B\uE06C\uE06D\uE06E\uE06F" +
                "\uE070\uE071\uE072\uE073\uE074\uE075\uE076\uE077" +
                "\uE078\uE079\uE07A\uE07B\uE07C\uE07D\uE07E\uE07F" +
                "\uE080\uE081\uE082\uE083\uE084\uE085\uE086\uE087" +
                "\uE088\uE089\uE08A\uE08B\uE08C\uE08D\uE08E\uE08F" +
                "\uE090\uE091\uE092\uE093\uE094\uE095\uE096\uE097" +
                "\uE098\uE099\uE09A\uE09B\uE09C\uE09D\uE09E\uE09F" +
                "\uE0A0\uE0A1\uE0A2\uE0A3\uE0A4\uE0A5\uE0A6\uE0A7" +
                "\uE0A8\uE0A9\uE0AA\uE0AB\uE0AC\uE0AD\uE0AE\uE0AF" +
                "\uE0B0\uE0B1\uE0B2\uE0B3\uE0B4\uE0B5\uE0B6\uE0B7" +
                "\uE0B8\uE0B9\uE0BA\uE0BB"
                ;
        }
    }

    protected static class Encoder extends CharsetEncoder {

        private static final char SBase = '\uAC00';
        private static final char LBase = '\u1100';
        private static final char VBase = '\u1161';
        private static final char TBase = '\u11A7';
        private static final int  VCount = 21;
        private static final int  TCount = 28;
        private static final byte G0 = 0;
        private static final byte G1 = 1;
        private static final byte G2 = 2;
        private static final byte G3 = 3;
        private byte   charState = G0;
        private char   l, v, t;
        private int mask1 , mask2, shift;

        private final Surrogate.Parser sgp = new Surrogate.Parser();

        public Encoder(Charset cs) {
            super(cs, 2.0f, 2.0f);
            mask1 = 0xFFF8;
            mask2 = 0x0007;
            shift = 3;
        }

        /**
         * Returns true if the given character can be converted to the
         * target character encoding.
         */
        public boolean canEncode(char ch) {
           int  index;
           int  theBytes;

           //The underneath data table returns a non-zero value for
           //some surrogates, here is the workaround.
           if (Surrogate.is(ch))
               return false;

           index = index1[((ch & mask1) >> shift)] + (ch & mask2);
           if (index < 15000)
             theBytes = (int)(index2.charAt(index));
           else
             theBytes = (int)(index2a.charAt(index-15000));

           if (theBytes != 0)
              return (true);

           // only return true if input char was unicode null - all others are
           //    undefined
           return( ch == '\u0000');
        }

        private CoderResult encodeArrayLoop(CharBuffer src, ByteBuffer dst) {
            char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            int sl = src.arrayOffset() + src.limit();
            byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            int dl = dst.arrayOffset() + dst.limit();
            int outputSize = 0;         // size of output
            byte[] outputBytes = new byte[2];

            boolean needsFlushing = false;

            try {
                while (sp < sl) {
                char inputChar = sa[sp];

                // Reject surrogates
                if (sgp.parse(inputChar, sa, sp, sl) < 0)
                    return sgp.error();
                if (Surrogate.is(inputChar))
                        return sgp.unmappableResult();

                switch (charState) {
                case G0:
                    l = LBase;
                    v = VBase;
                    t = TBase;

                    if ( isLeadingC(inputChar) ) {     // Leading Consonant
                        l = inputChar;
                        charState = G1;
                        break;
                    }

                    if ( isVowel(inputChar) ) {        // Vowel
                        v = inputChar;
                        charState = G2;
                        break;
                    }

                    if ( isTrailingC(inputChar) ) {    // Trailing Consonant
                        t = inputChar;
                        charState = G3;
                        break;
                    }

                    break;

                    case G1:
                        if ( isLeadingC(inputChar) ) {     // Leading Consonant
                            l = composeLL(l, inputChar);
                            needsFlushing = true;
                        break;
                        }

                        if ( isVowel(inputChar) ) {        // Vowel
                            v = inputChar;
                            charState = G2;
                            needsFlushing = true;
                            break;
                        }

                        if ( isTrailingC(inputChar) ) {    // Trailing Consonant
                            t = inputChar;
                            charState = G3;
                            needsFlushing = true;
                            break;
                        }

                        charState = G0;
                        break;

                    case G2:
                        if ( isLeadingC(inputChar) ) {     // Leading Consonant
                            needsFlushing = true;

                            l = inputChar;
                            v = VBase;
                            t = TBase;
                            charState = G1;
                            break;
                        }

                        if ( isVowel(inputChar) ) {        // Vowel
                            needsFlushing = true;
                            v = composeVV(l, inputChar);
                            charState = G2;
                            break;
                        }

                        if ( isTrailingC(inputChar) ) {    // Trailing Consonant
                            needsFlushing = true;
                            t = inputChar;
                            charState = G3;
                            break;
                        }

                        charState = G0;
                        break;

                    case G3:
                        if ( isTrailingC(inputChar) ) {    // Trailing Consonant
                            needsFlushing = true;
                            t = composeTT(t, inputChar);
                            charState = G3;
                            break;
                        }

                        charState = G0;
                        break;
                  }

                  if (charState == G0 || (sl - sp < 2)) {
                      int spaceNeeded;

                        if (needsFlushing) {
                            needsFlushing = false;
                            outputBytes = encodeHangul(composeHangul());

                            if (outputBytes[0] == 0x00 && outputBytes[1] == 0x00
                                && inputChar != '\u0000') {
                                  return CoderResult.unmappableForLength(1);
                            }

                            if (outputBytes[0] == 0x00)
                                spaceNeeded = 1;
                            else
                                spaceNeeded = 2;

                            if (dl - dp < spaceNeeded)
                                 return CoderResult.OVERFLOW;
                              if (spaceNeeded == 1)
                                    da[dp++] = outputBytes[1];
                              else {
                                    da[dp++] = outputBytes[0];
                                    da[dp++] = outputBytes[1];
                              }
                           }
                        outputBytes = encodeHangul(inputChar);
                        if (outputBytes[0] == 0x00 && outputBytes[1] == 0x00
                            && inputChar != '\u0000') {
                              return CoderResult.unmappableForLength(1);
                        }

                        if (outputBytes[0] == 0x00)
                            spaceNeeded = 1;
                        else
                            spaceNeeded = 2;

                        if (dl - dp < spaceNeeded) {
                             return CoderResult.OVERFLOW;
                        }
                          if (spaceNeeded == 1)
                                da[dp++] = outputBytes[1];
                          else {
                                da[dp++] = outputBytes[0];
                                    da[dp++] = outputBytes[1];
                              }
                        }
                  sp++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private CoderResult encodeBufferLoop(CharBuffer src, ByteBuffer dst) {
            int mark = src.position();
            int outputSize = 0;
            byte[] outputBytes = new byte[2];
            boolean needsFlushing = false;

            try {
                while (src.hasRemaining()) {
                    char inputChar = src.get();
                    if (Surrogate.is(inputChar)) {
                        if (sgp.parse(inputChar, src) < 0)
                            return sgp.error();
                    return sgp.unmappableResult();
                    }

                    if (inputChar >= '\uFFFE')
                        return CoderResult.unmappableForLength(1);

                    switch (charState) {
                    case G0:
                        l = LBase;
                        v = VBase;
                        t = TBase;

                        if ( isLeadingC(inputChar) ) {     // Leading Consonant
                            l = inputChar;
                            charState = G1;
                            break;
                        }

                        if ( isVowel(inputChar) ) {        // Vowel
                            v = inputChar;
                            charState = G2;
                            break;
                        }

                        if ( isTrailingC(inputChar) ) {    // Trailing Consonant
                            t = inputChar;
                            charState = G3;
                            break;
                        }

                        break;

                        case G1:
                            if ( isLeadingC(inputChar) ) {     // Leading Consonant
                                l = composeLL(l, inputChar);
                                needsFlushing = true;
                            break;
                            }

                            if ( isVowel(inputChar) ) {        // Vowel
                                v = inputChar;
                                charState = G2;
                                needsFlushing = true;
                                break;
                            }

                            if ( isTrailingC(inputChar) ) {    // Trailing Consonant
                                t = inputChar;
                                charState = G3;
                                needsFlushing = true;
                                break;
                            }

                            charState = G0;
                            break;

                        case G2:
                            if ( isLeadingC(inputChar) ) {     // Leading Consonant
                                needsFlushing = true;

                                l = inputChar;
                                v = VBase;
                                t = TBase;
                                charState = G1;
                                break;
                            }

                            if ( isVowel(inputChar) ) {        // Vowel
                                needsFlushing = true;
                                v = composeVV(l, inputChar);
                                charState = G2;
                                break;
                            }

                            if ( isTrailingC(inputChar) ) {    // Trailing Consonant
                                needsFlushing = true;
                                t = inputChar;
                                charState = G3;
                                break;
                            }

                            charState = G0;
                            break;

                        case G3:
                            if ( isTrailingC(inputChar) ) {    // Trailing Consonant
                                needsFlushing = true;
                                t = composeTT(t, inputChar);
                                charState = G3;
                                break;
                            }

                            charState = G0;
                            break;
                      }

                      if (charState == G0 || src.remaining() <= 0) {
                          int spaceNeeded;

                          if (needsFlushing) {
                            needsFlushing = false;

                            outputBytes = encodeHangul(composeHangul());
                            if (outputBytes[0] == 0x00
                                && outputBytes[1] == 0x00
                                && inputChar != '\u0000') {
                                return CoderResult.unmappableForLength(1);
                            }

                            if (outputBytes[0] == 0x00)
                                spaceNeeded = 1;
                            else
                                spaceNeeded = 2;

                            if (dst.remaining() < spaceNeeded)
                                 return CoderResult.OVERFLOW;
                              if (spaceNeeded == 1)
                                    dst.put(outputBytes[1]);
                              else {
                                    dst.put(outputBytes[0]);
                                    dst.put(outputBytes[1]);
                              }
                          }

                          outputBytes = encodeHangul(inputChar);
                          if (outputBytes[0] == 0x00 && outputBytes[1] == 0x00
                              && inputChar != '\u0000') {
                              return CoderResult.unmappableForLength(1);
                          }

                          if (outputBytes[0] == 0x00)
                              spaceNeeded = 1;
                          else
                              spaceNeeded = 2;

                          if (dst.remaining() < spaceNeeded)
                                return CoderResult.OVERFLOW;

                          if (spaceNeeded == 1)
                                dst.put(outputBytes[1]);
                          else {
                                dst.put(outputBytes[0]);
                                dst.put(outputBytes[1]);
                            }
                        }
                      mark++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        private char composeHangul() {
           int lIndex, vIndex, tIndex;

           lIndex = l - LBase;
           vIndex = v - VBase;
           tIndex = t - TBase;

           return (char)((lIndex * VCount + vIndex) * TCount + tIndex + SBase);
        }

        private char composeLL(char l1, char l2) {
           return l2;
        }

        private char composeVV(char v1, char v2) {
           return v2;
        }

        private char composeTT(char t1, char t2) {
           return t2;
        }

        private boolean isLeadingC(char c) {
           return (c >= LBase && c <= '\u1159');
        }

        private boolean isVowel(char c) {
           return (c >= VBase && c <= '\u11a2');
        }

        private boolean isTrailingC(char c) {
           return (c >= TBase && c <= '\u11f9');
        }

        private byte[] encodeHangul(char unicode) {
            int index;
            byte[] outputBytes = new byte[2];

            int theBytes;

            // first we convert the unicode to its byte representation

            index = index1[((unicode & mask1) >> shift)] + (unicode & mask2);
            if (index < 15000)
             theBytes = (int)(index2.charAt(index));
            else
             theBytes = (int)(index2a.charAt(index-15000));

            outputBytes[0] = (byte)((theBytes & 0x0000ff00)>>8);
            outputBytes[1] = (byte)(theBytes & 0x000000ff);

            return outputBytes;
        }

        protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
            if (true && src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }

        private static final short index1[] =
        {
                 2751,   771,   710,   642,    41, 20287, 19487, 19075, // 0000 - 003F
                15804, 13732, 12911, 12503, 12212, 11716, 11086,  8904, // 0040 - 007F
                 6712,  4859,  3337,  1411,  2695, 20484, 20279, 20234, // 0080 - 00BF
                14112, 19002, 20155, 19929, 13922, 19002, 19844, 19479, // 00C0 - 00FF
                19002, 19002,  2553, 19002, 13857, 19002,  2491, 19270, // 0100 - 013F
                19058,  2376,  9341, 19002, 13804, 19002, 19002, 19002, // 0140 - 017F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0180 - 01BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 01C0 - 01FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0200 - 023F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0240 - 027F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0280 - 02BF
                19003, 19002, 19001, 18783, 19002, 19002, 19002, 19002, // 02C0 - 02FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0300 - 033F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0340 - 037F
                19002, 19002,  2328, 18663, 18523, 18472,  2313, 18383, // 0380 - 03BF
                18272, 18138, 19002, 19002, 19002, 19002, 19002, 19002, // 03C0 - 03FF
                 2201, 19002, 18020, 17958, 17671, 17506, 17138, 16569, // 0400 - 043F
                16474, 16386,  2137, 19002, 19002, 19002, 19002, 19002, // 0440 - 047F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0480 - 04BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 04C0 - 04FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0500 - 053F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0540 - 057F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0580 - 05BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 05C0 - 05FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0600 - 063F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0640 - 067F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0680 - 06BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 06C0 - 06FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0700 - 073F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0740 - 077F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0780 - 07BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 07C0 - 07FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0800 - 083F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0840 - 087F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0880 - 08BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 08C0 - 08FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0900 - 093F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0940 - 097F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0980 - 09BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 09C0 - 09FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0A00 - 0A3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0A40 - 0A7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0A80 - 0ABF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0AC0 - 0AFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0B00 - 0B3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0B40 - 0B7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0B80 - 0BBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0BC0 - 0BFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0C00 - 0C3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0C40 - 0C7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0C80 - 0CBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0CC0 - 0CFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0D00 - 0D3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0D40 - 0D7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0D80 - 0DBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0DC0 - 0DFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0E00 - 0E3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0E40 - 0E7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0E80 - 0EBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0EC0 - 0EFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0F00 - 0F3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0F40 - 0F7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0F80 - 0FBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 0FC0 - 0FFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1000 - 103F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1040 - 107F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1080 - 10BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 10C0 - 10FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1100 - 113F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1140 - 117F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1180 - 11BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 11C0 - 11FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1200 - 123F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1240 - 127F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1280 - 12BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 12C0 - 12FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1300 - 133F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1340 - 137F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1380 - 13BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 13C0 - 13FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1400 - 143F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1440 - 147F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1480 - 14BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 14C0 - 14FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1500 - 153F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1540 - 157F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1580 - 15BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 15C0 - 15FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1600 - 163F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1640 - 167F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1680 - 16BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 16C0 - 16FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1700 - 173F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1740 - 177F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1780 - 17BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 17C0 - 17FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1800 - 183F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1840 - 187F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1880 - 18BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 18C0 - 18FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1900 - 193F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1940 - 197F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1980 - 19BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 19C0 - 19FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1A00 - 1A3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1A40 - 1A7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1A80 - 1ABF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1AC0 - 1AFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1B00 - 1B3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1B40 - 1B7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1B80 - 1BBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1BC0 - 1BFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1C00 - 1C3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1C40 - 1C7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1C80 - 1CBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1CC0 - 1CFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1D00 - 1D3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1D40 - 1D7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1D80 - 1DBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1DC0 - 1DFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1E00 - 1E3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1E40 - 1E7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1E80 - 1EBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1EC0 - 1EFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1F00 - 1F3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1F40 - 1F7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1F80 - 1FBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 1FC0 - 1FFF
                19002, 19002, 16346, 16100, 15796, 19002, 15230, 11945, // 2000 - 203F
                19002, 19002, 19002, 19002, 19002, 19002,  8657, 18834, // 2040 - 207F
                 2052, 19002, 19002, 19002, 19002, 20710, 19002, 19002, // 2080 - 20BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 20C0 - 20FF
                11959,  1989,  9248, 19002,  1958,  9001, 19002, 19002, // 2100 - 213F
                19002, 19002, 18361,  8885, 14927, 14669, 14506, 14423, // 2140 - 217F
                19002, 19002, 14007, 13714, 19002, 19002, 19002, 19002, // 2180 - 21BF
                19002, 19002,  9179, 19002, 19002, 19002, 19002, 19002, // 21C0 - 21FF
                13605, 13116,  1861,  4692, 13037, 12903,  3903,  3439, // 2200 - 223F
                19002, 19002, 18335, 19002, 12566,  9927, 19002, 19002, // 2240 - 227F
                19638, 19002, 19002, 19002,  3396, 19002, 19002, 19002, // 2280 - 22BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 22C0 - 22FF
                19002, 19002,  4529, 19002, 19002, 19002, 19002, 19002, // 2300 - 233F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2340 - 237F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2380 - 23BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 23C0 - 23FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2400 - 243F
                19002, 19002, 19002, 19002, 12529, 12495,  8234, 12405, // 2440 - 247F
                12190, 19002, 19002,  8215, 12024, 11881, 11697, 19002, // 2480 - 24BF
                19002, 19002, 11531, 11480, 11382, 11214, 19002, 19002, // 24C0 - 24FF
                11067,  8156, 10970, 10466, 10369, 10258, 10194, 10028, // 2500 - 253F
                 9903,  9776, 19002, 19002, 19002, 19002, 19002, 19002, // 2540 - 257F
                19002, 19002, 19599, 19002,  9752,  9554, 18173, 16194, // 2580 - 25BF
                 9256,  9072,  8893, 19002, 19002, 19002, 19002, 19002, // 25C0 - 25FF
                 3377, 13771, 19002, 15873, 19002, 19002, 19002, 19002, // 2600 - 263F
                 8728, 19002, 19002, 19002,  8684,  8571, 19002, 19002, // 2640 - 267F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2680 - 26BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 26C0 - 26FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2700 - 273F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2740 - 277F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2780 - 27BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 27C0 - 27FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2800 - 283F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2840 - 287F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2880 - 28BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 28C0 - 28FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2900 - 293F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2940 - 297F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2980 - 29BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 29C0 - 29FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2A00 - 2A3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2A40 - 2A7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2A80 - 2ABF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2AC0 - 2AFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2B00 - 2B3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2B40 - 2B7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2B80 - 2BBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2BC0 - 2BFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2C00 - 2C3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2C40 - 2C7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2C80 - 2CBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2CC0 - 2CFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2D00 - 2D3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2D40 - 2D7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2D80 - 2DBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2DC0 - 2DFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2E00 - 2E3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2E40 - 2E7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2E80 - 2EBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2EC0 - 2EFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2F00 - 2F3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2F40 - 2F7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2F80 - 2FBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 2FC0 - 2FFF
                 8426,  8260,  8040, 15735, 19002, 19002, 19002, 19002, // 3000 - 303F
                 1649,  7754,  7652,  7598,  7450,  7334,  7152,  7086, // 3040 - 307F
                 7017,  6818,  6677, 19002,  1807,  6624,  6502,  6324, // 3080 - 30BF
                 6233,  5853,  5749,  5332,  5244,  5010,  4851,  7296, // 30C0 - 30FF
                19002, 19002, 19002, 19002, 19002, 19002,  1792,  4708, // 3100 - 313F
                 4627,  4460,  4430,  4250,  4224,  4172,  3785,  3664, // 3140 - 317F
                 3578,  3329, 19002, 19002, 19002, 19002, 19002, 19002, // 3180 - 31BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 31C0 - 31FF
                 3224,  3029,  2572,  2460, 19002, 19002, 19002, 19002, // 3200 - 323F
                19002, 19002, 19002, 19002,  2419,  2257,  2175,  1974, // 3240 - 327F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3280 - 32BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 32C0 - 32FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3300 - 333F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3340 - 337F
                 1382,  1276,  1140,   948,   914,   831,   442, 20683, // 3380 - 33BF
                20614, 20562, 20476, 20435, 19002, 19002, 19002, 19002, // 33C0 - 33FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3400 - 343F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3440 - 347F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3480 - 34BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 34C0 - 34FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3500 - 353F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3540 - 357F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3580 - 35BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 35C0 - 35FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3600 - 363F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3640 - 367F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3680 - 36BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 36C0 - 36FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3700 - 373F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3740 - 377F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3780 - 37BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 37C0 - 37FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3800 - 383F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3840 - 387F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3880 - 38BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 38C0 - 38FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3900 - 393F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3940 - 397F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3980 - 39BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 39C0 - 39FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3A00 - 3A3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3A40 - 3A7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3A80 - 3ABF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3AC0 - 3AFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3B00 - 3B3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3B40 - 3B7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3B80 - 3BBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3BC0 - 3BFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3C00 - 3C3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3C40 - 3C7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3C80 - 3CBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3CC0 - 3CFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3D00 - 3D3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3D40 - 3D7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3D80 - 3DBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3DC0 - 3DFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3E00 - 3E3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3E40 - 3E7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3E80 - 3EBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3EC0 - 3EFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3F00 - 3F3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3F40 - 3F7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3F80 - 3FBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 3FC0 - 3FFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4000 - 403F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4040 - 407F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4080 - 40BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 40C0 - 40FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4100 - 413F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4140 - 417F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4180 - 41BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 41C0 - 41FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4200 - 423F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4240 - 427F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4280 - 42BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 42C0 - 42FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4300 - 433F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4340 - 437F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4380 - 43BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 43C0 - 43FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4400 - 443F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4440 - 447F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4480 - 44BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 44C0 - 44FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4500 - 453F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4540 - 457F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4580 - 45BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 45C0 - 45FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4600 - 463F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4640 - 467F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4680 - 46BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 46C0 - 46FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4700 - 473F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4740 - 477F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4780 - 47BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 47C0 - 47FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4800 - 483F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4840 - 487F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4880 - 48BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 48C0 - 48FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4900 - 493F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4940 - 497F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4980 - 49BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 49C0 - 49FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4A00 - 4A3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4A40 - 4A7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4A80 - 4ABF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4AC0 - 4AFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4B00 - 4B3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4B40 - 4B7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4B80 - 4BBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4BC0 - 4BFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4C00 - 4C3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4C40 - 4C7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4C80 - 4CBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4CC0 - 4CFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4D00 - 4D3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4D40 - 4D7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4D80 - 4DBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 4DC0 - 4DFF
                20419, 20303,  1697, 20271, 19002,  3278,  9727, 20215, // 4E00 - 4E3F
                17471,  8801, 13421, 20182, 19002, 15114, 18218, 13347, // 4E40 - 4E7F
                19254, 20147, 20124, 19921,  1664, 19836, 13194,  4030, // 4E80 - 4EBF
                19773, 13078,  3620, 19757, 15394, 19002, 19699, 18111, // 4EC0 - 4EFF
                 1568,  1553, 19680, 19002, 19002, 16907,  7542, 19454, // 4F00 - 4F3F
                18064, 19415, 19342,  1509, 19002,  1538, 19262, 12793, // 4F40 - 4F7F
                 1449, 19205,  1359, 19162, 19002, 13184,  3168, 16210, // 4F80 - 4FBF
                17257,  1291,  1483, 17196, 19110, 13160,  1228, 19036, // 4FC0 - 4FFF
                17122,  1155,  1113,  1052,  1332, 18983, 19002, 14850, // 5000 - 503F
                18033, 18913,  3087,  8505,  3069, 19002, 15343, 18833, // 5040 - 507F
                18768,  2874,  1034, 18715, 19002, 15017,  8459, 13095, // 5080 - 50BF
                 2706,  1019,  1246,  8365,  2680,  2634, 19002,  1004, // 50C0 - 50FF
                18655,   963,  3850, 15772,   929,  9876, 18588,  4359, // 5100 - 513F
                18604, 18515, 18545, 14834,  9103, 18464,   846, 18438, // 5140 - 517F
                18375,  8115, 15998, 19002, 18234, 15701, 13047,  2612, // 5180 - 51BF
                 7070,   804, 19002, 14600,   752, 19002, 18130, 18005, // 51C0 - 51FF
                17981, 17950,   729,  2594,  6893,   868, 17919, 17889, // 5200 - 523F
                 2099,  7676, 14407,  6662,   691,   676,  3609,  2583, // 5240 - 527F
                 6361, 17861,  4866, 13411, 17803,  4877, 19002, 13021, // 5280 - 52BF
                18790, 18482, 18367, 17787, 18313, 19002, 14035, 17748, // 52C0 - 52FF
                 2517, 17726, 17687, 18489, 17659,  7622, 19002, 18499, // 5300 - 533F
                17579, 17522, 18148,  7582, 17498, 17350, 17298,  2476, // 5340 - 537F
                14209, 19002, 13908, 17227, 17130,  2287, 19002,  5830, // 5380 - 53BF
                13305, 17085,  6792,  5214, 16107, 16114, 17026, 16906, // 53C0 - 53FF
                 5702, 16778, 16717,  5642, 16561, 15241, 17240, 16466, // 5400 - 543F
                 7510, 16420, 14679, 19002, 19002, 16367, 14689, 17183, // 5440 - 547F
                16338, 13744, 16209, 19002,  6763, 16139,  5189, 16023, // 5480 - 54BF
                15953, 15881, 19002, 19002, 14696, 15788,  7306, 14757, // 54C0 - 54FF
                13271, 12960, 15771,  6390, 19002, 15593, 14703, 12883, // 5500 - 553F
                 6123, 15547, 19828, 12803, 20664, 19002, 19002, 12715, // 5540 - 557F
                15749, 14710, 19002, 15636, 15331, 13724, 19002, 19002, // 5580 - 55BF
                 2186, 19002, 12606,  3267, 20399, 19002, 19002,  2028, // 55C0 - 55FF
                12586, 12573,  6082, 19002, 19002, 14813,  3310, 19002, // 5600 - 563F
                19002, 19002, 16836, 19002, 19002, 15609,  5990, 19002, // 5640 - 567F
                12513, 19002, 19002, 19002,  1916, 11843,  5884,  5774, // 5680 - 56BF
                19002,  6842, 12201, 17853, 15592, 19002, 15546, 11704, // 56C0 - 56FF
                12429, 15488, 17779, 15449, 19002, 15351, 15330,  3999, // 5700 - 573F
                15287, 17740, 15261, 19002, 11078, 17718, 15215,  2835, // 5740 - 577F
                 6482, 15050, 10315, 19002, 14985, 19002, 19002, 19002, // 5780 - 57BF
                   25, 14919, 19002, 12812, 14812, 19002, 14781,  9564, // 57C0 - 57FF
                14661, 14608, 19002, 12472,  8757,  6458, 14574, 17633, // 5800 - 583F
                19002,  6209,  8578, 14498,  8047, 19002,  1886,  1403, // 5840 - 587F
                20647, 20442, 16487,  5547, 19002, 14415,  1765,  5662, // 5880 - 58BF
                20452, 12444, 20310, 14395, 11192, 16442, 19002, 20317, // 58C0 - 58FF
                19002, 12375, 11098, 20324, 12839, 20226, 20131, 12366, // 5900 - 593F
                10885, 14353, 14311,  5117, 14120,  5034, 11744, 13930, // 5940 - 597F
                11902, 16961,  1589, 19461,  1818, 13842, 19002, 19471, // 5980 - 59BF
                 1268, 19349, 13752, 19169, 12324, 13706, 19002, 13643, // 59C0 - 59FF
                19176, 19002, 19120, 13627, 13565, 18990, 12240,  2791, // 5A00 - 5A3F
                18775, 18725, 19002, 16938, 10954, 19002, 19002, 19002, // 5A40 - 5A7F
                19002, 19002, 10920, 10820,  4676, 19002, 19002, 19002, // 5A80 - 5ABF
                18735, 18241, 19002, 19002, 18012, 17900, 19002, 19002, // 5AC0 - 5AFF
                 1631, 17868, 12108, 19002, 19002,  4274, 19002, 19002, // 5B00 - 5B3F
                13451, 19002, 13429, 13355,   429, 17529, 13327, 13240, // 5B40 - 5B7F
                 1422, 13202, 19624, 13103, 10693, 10070, 13029, 13011, // 5B80 - 5BBF
                12968,  4513, 12950, 12080, 17092, 12927,  1193, 12891, // 5BC0 - 5BFF
                17037, 12873, 16378, 16030,  9597, 12811, 16040, 12594, // 5C00 - 5C3F
                12558, 12521, 16050, 20580, 12480,  9585, 16057, 16067, // 5C40 - 5C7F
                19002, 19002, 12452, 19002, 16077, 16087, 15969, 12397, // 5C80 - 5CBF
                12374, 19002, 19002, 15643, 12332, 12248, 12182, 19516, // 5CC0 - 5CFF
                12333,   977, 15620, 15495, 12117, 15505, 19002, 19002, // 5D00 - 5D3F
                19002,  7480, 12116, 19002, 19002, 15515, 19002, 19002, // 5D40 - 5D7F
                10720, 15414, 19002,   887, 12007, 16766, 19002, 11924, // 5D80 - 5DBF
                19002,   782,  2403,   653, 15358, 11873, 15271,  2556, // 5DC0 - 5DFF
                16709,  4333, 15222, 15061, 18475,  7249, 14303, 11851, // 5E00 - 5E3F
                11762,  9466, 18141, 10556, 15068, 19002, 16695, 11689, // 5E40 - 5E7F
                 4083,  4921,  2204, 16631, 11553, 13143,  2140, 11523, // 5E80 - 5EBF
                 3823, 11463, 11374, 16545, 11320,  4014, 12658,  9694, // 5EC0 - 5EFF
                15084,  9454, 15005, 11263, 11978, 14619, 14629,  8663, // 5F00 - 5F3F
                19002, 11206, 11658, 14636,  3491, 14581, 11144, 14588, // 5F40 - 5F7F
                11128,  3361, 11106, 11059, 11034, 10962,  1992, 13937, // 5F80 - 5FBF
                12293,  9146, 11630, 10755, 10719,  5924, 14672, 14426, // 5FC0 - 5FFF
                19002,  9510,  3201,  3941, 10701, 10647, 19002, 19002, // 6000 - 603F
                13944, 13717, 10618, 13964, 16162, 10574, 10555, 19002, // 6040 - 607F
                 1864, 13971,  8623,  8014, 10488, 19002, 10458, 10430, // 6080 - 60BF
                11217, 19002, 13978,  1939, 10387, 19002, 10361, 13988, // 60C0 - 60FF
                13999, 10337,  9557, 15898,  8632, 19002, 10281,  3873, // 6100 - 613F
                15861, 10245,  5417, 10223, 15838, 10186, 10101,  8896, // 6140 - 617F
                13572,  7738, 10078, 10016,  3759, 13597, 15763, 15741, // 6180 - 61BF
                 5352,  9989, 19002, 19002, 11515, 19002,  7689,  9971, // 61C0 - 61FF
                 9953,  9895,  9768, 15676, 13334, 15584,  9744, 11490, // 6200 - 623F
                 9629,  9536, 10537,  9509, 19002, 11455, 12487, 12462, // 6240 - 627F
                 9417, 11931, 11345,  9368, 19002,  4490, 10765, 10772, // 6280 - 62BF
                15561,  9296,  9278,  9238, 19002,  8186, 10779, 11417, // 62C0 - 62FF
                10625, 10437, 10397, 19002, 19002,  8391, 19002, 15538, // 6300 - 633F
                19002, 10407,  9199, 19002,  8597,  9064,  7486, 15472, // 6340 - 637F
                 3290,  8986, 15441,  8871,  8849,  8787, 19002, 19002, // 6380 - 63BF
                 8720,  8543,  8676, 15380,  9543,  1571,  3726, 19002, // 63C0 - 63FF
                19002, 19683,  7975,  3597, 15154,  3450, 19002, 15042, // 6400 - 643F
                19002, 19002, 19002,  8631,  8596,  9433, 19002,  8559, // 6440 - 647F
                19002,  8542,  9379, 14977,  7531,  6308,  8477,  5578, // 6480 - 64BF
                 9395, 14911, 14868,  8409,  9206,  3407, 14840, 14804, // 64C0 - 64FF
                 8373, 19002, 19002,  8326,  2665, 14736, 11180,  8300, // 6500 - 653F
                19113,  8282,  9012, 11026, 14566,  3243, 14543,  8252, // 6540 - 657F
                14529,  2088,  8174,  8333,  7890,  2341,  8091,  7883, // 6580 - 65BF
                 7876,  1620, 14482, 19002,  8032,  7869,  7815, 14440, // 65C0 - 65FF
                10895,  7827,  7808, 10789,  7790,  7772,  7746, 14376, // 6600 - 663F
                 7437,  7366, 19002,  7189,  6594,  7707,  1182,  7640, // 6640 - 667F
                 3098,  7590,  7550,  7430,  7350,   411, 14325,  7144, // 6680 - 66BF
                 6189,  7179, 10711,  7137,  7326, 19002,  7267,  7207, // 66C0 - 66FF
                 7168,  7130,  3058,   134, 10657,  7078,  6669, 14236, // 6700 - 673F
                10566,  6582,  7009,  2947, 19002,  1037,  6973, 20493, // 6740 - 677F
                 6154,  6553,  6921, 14215, 19002,  5961,  6874,  6810, // 6780 - 67BF
                 2885,  5522,  6647, 14190, 19002,  6416,  6616, 19067, // 67C0 - 67FF
                19002, 19002, 14134, 19002,  6041, 14102, 19002,  6575, // 6800 - 683F
                 6542,  6494,  6404, 19002, 19002, 18718, 10450,  5071, // 6840 - 687F
                 6034,  3980,  9350,   966,  6177,  6342,  6316, 19002, // 6880 - 68BF
                 2824,  6027, 14088,  6295,  6279,  6225,  2979,  5841, // 68C0 - 68FF
                 6170,   932, 14021, 19002,  3177, 19002,  6153,   755, // 6900 - 693F
                19002, 13894, 16351,  5741,  6131,  6104,   694,  6017, // 6940 - 697F
                19002, 19002, 18492,  2057, 16720, 10379,  5395,  2062, // 6980 - 69BF
                 5714,  9184,  5960, 19002, 19002,  5816, 19002,  9785, // 69C0 - 69FF
                13871, 13818,  5614, 20017,  5200, 19002, 14682,  5734, // 6A00 - 6A3F
                 2717,  5688, 13785,  5670,  5049,  8737, 19002, 19002, // 6A40 - 6A7F
                 5632, 20675,  2037,  5274,  6769, 19002,  8746,  8435, // 6A80 - 6ABF
                13684, 19002,  6686, 13661, 19002, 19002, 10329,  6695, // 6AC0 - 6AFF
                 5101,  2923, 13619, 19002,  5603, 19002, 13548, 13443, // 6B00 - 6B3F
                10125,  4971,  5521, 20338,  3656, 13369, 13285,  5451, // 6B40 - 6B7F
                 6704,  4753, 10093,  5324, 19002, 10058, 13259, 13216, // 6B80 - 6BBF
                20411,  2465, 13174,  5302, 19002,  1387, 19002, 19002, // 6BC0 - 6BFF
                19002,  5262, 20006, 19002,  1392, 19002,  2645,  5236, // 6C00 - 6C3F
                 5175,  9981,  5089, 13130,  5070,  5042,  5002, 13003, // 6C40 - 6C7F
                11831,  4979,  4843, 19873, 19002,  9736,  1514,  4796, // 6C80 - 6CBF
                19818, 20654,  1523, 17262,  3649,  4780,  4742, 19002, // 6CC0 - 6CFF
                19002, 19045, 19399,  3216, 15884,  3153,  6396,  4700, // 6D00 - 6D3F
                19323, 19002, 19002, 20389, 19050, 18905,  4893,  4643, // 6D40 - 6D7F
                12576,  4619, 19999, 19002, 19002, 20092, 12982, 19002, // 6D80 - 6DBF
                 4551,  8510,  2246,  4452, 11821,  4422,  1682,  4382, // 6DC0 - 6DFF
                19002, 19002, 20070, 11620,  4242,  4364, 12865,  4216, // 6E00 - 6E3F
                18554, 12826,  9963,  4198, 20042, 18559, 12751, 12721, // 6E40 - 6E7F
                19002, 19002,  4164,  2623, 12672, 12630, 19913, 12612, // 6E80 - 6EBF
                12543,  1374,  1208, 19002, 19002, 19782,  4655,  4146, // 6EC0 - 6EFF
                 1132, 19741, 19866, 19002,  4101, 18443, 19807,  4064, // 6F00 - 6F3F
                19721, 19002, 15192,  4042, 12419, 12204,  3979, 12389, // 6F40 - 6F7F
                   15,  3777,  3697, 19002, 18448,  9835, 20633, 20606, // 6F80 - 6FBF
                 3638, 12276, 15264, 12262,  3565, 20591, 19523,  9528, // 6FC0 - 6FFF
                19446, 20538, 19392,  3527, 15706,  3385, 19664,  9501, // 7000 - 703F
                19002,  1840,  3318,  3209, 17808, 18318,  3176,  3146, // 7040 - 707F
                20445, 12174, 12131,  3120, 14285,  7519, 16429,  3018, // 7080 - 70BF
                19002,  2996, 19002,  2863, 19002, 19002, 19313, 20382, // 70C0 - 70FF
                 2528, 20358, 19002, 17571, 17490, 19002,  2564, 19002, // 7100 - 713F
                19655, 20263,  2452, 20174,  2506, 20116, 19002, 19464, // 7140 - 717F
                 2438, 12703,  5996, 20061, 12046,  2391, 18895, 18825, // 7180 - 71BF
                19988,  2411,  2239, 19424, 19123, 18993, 19002, 16144, // 71C0 - 71FF
                 2167, 19002,  2036, 15958, 19002, 11992,  1966, 11865, // 7200 - 723F
                 1905,  1755, 11776,  1705, 19962, 19944, 11730, 11814, // 7240 - 727F
                 1672, 19002, 19002, 19002,  5890,  2298, 19002, 19002, // 7280 - 72BF
                 1579,  9360,  1457, 11610, 19903, 10849, 19002,  1367, // 72C0 - 72FF
                19002, 11672,  9288, 14766, 18728, 16810,  9216,  9113, // 7300 - 733F
                 3885, 19002,  1299, 19002, 19002,  1254,  1201,  1121, // 7340 - 737F
                 2276, 10178,  2223,  1079, 19279, 19859, 11644, 19797, // 7380 - 73BF
                  985,  9669,  8978,  9191,   940, 19714, 19084, 15945, // 73C0 - 73FF
                 7412, 15182, 14994,  9569,   906,   876,   823,   661, // 7400 - 743F
                  419, 17642, 17903, 15131,     8, 20699, 19002,  8841, // 7440 - 747F
                11567, 19610, 19002, 20691, 14963, 20622, 20599, 19561, // 7480 - 74BF
                 8712,  5780,  3709, 20570, 20554,  8588, 19012, 19002, // 74C0 - 74FF
                19506, 19002, 19439, 20546, 17651, 20526, 20518, 20501, // 7500 - 753F
                18851,  3539, 18093, 19385, 11545, 11445, 20468, 20460, // 7540 - 757F
                 8520,  2841, 14275, 16033, 16043,  8052, 19306, 20427, // 7580 - 75BF
                11431, 16060, 11359, 20371, 11308, 19002, 20346, 11281, // 75C0 - 75FF
                20337, 16070, 19002, 14167, 20295, 19002, 19002,  8061, // 7600 - 763F
                11243,  2159, 11198, 19002, 17559, 17478, 20242, 20207, // 7640 - 767F
                 2122,  8469, 20190, 19002, 19002,  8401, 19002, 11170, // 7680 - 76BF
                11120, 20163, 11051, 20329, 19228,  8318, 10984, 20139, // 76C0 - 76FF
                19138, 20100, 19002,  8292, 20091, 17413, 18799, 20078, // 7700 - 773F
                20069, 12455, 19002,  5126, 19102,  5135, 19002, 13085, // 7740 - 777F
                19002,  5144, 12693,  8244, 16080,  2077, 20050,  5153, // 7780 - 77BF
                19002, 19002, 18407, 11907, 10866, 12314, 11912, 19002, // 77C0 - 77FF
                 2797, 19002, 10803, 19002, 15646,  2011, 10734, 19002, // 7800 - 783F
                15498, 18348, 19002, 15508, 19002, 13847, 19002,  1927, // 7840 - 787F
                18959,  1897, 18932, 19002, 19358, 18885, 19002, 10671, // 7880 - 78BF
                18818, 10639, 20041, 19002, 19002, 20033, 14622, 13757, // 78C0 - 78FF
                18704,  8166,  8024, 19002, 19002, 10588, 19002, 10547, // 7900 - 793F
                20025, 19978, 19970, 10480, 19955, 19937, 19002, 10295, // 7940 - 797F
                18686, 11037, 11804, 19002,  7908, 10237, 11599, 11016, // 7980 - 79BF
                19893, 10838, 16803, 19885,  2959, 10168, 19002, 19185, // 79C0 - 79FF
                19852, 19790,  2903, 16595, 19781,  7800, 16505, 16947, // 7A00 - 7A3F
                19765,  9887, 18299, 19002,  9658,  9331, 19749, 18574, // 7A40 - 7A7F
                 9173, 19740, 10208, 19732, 19002,  8968, 19002,  4682, // 7A80 - 7ABF
                 1875, 10139, 18290, 16299, 19707, 10115,  7782, 16241, // 7AC0 - 7AFF
                19002, 18281, 16179, 18430, 19691,  1829, 19002, 15935, // 7B00 - 7B3F
                 7764, 18398, 19672, 19002, 19663,  1747, 10758, 13981, // 7B40 - 7B7F
                18182, 10929,  1724,  4590,  7402, 10390, 18329, 19654, // 7B80 - 7BBF
                19646, 15172,  9917, 19002, 19632, 15121, 19002, 19002, // 7BC0 - 7BFF
                18051, 19002,  4519,  7699, 18197,  9849, 19002, 13337, // 7C00 - 7C3F
                18740,  2111, 19002, 19002, 19593,  1951, 18246, 19002, // 7C40 - 7C7F
                18255, 14953,  9799, 19585, 10440,  7632, 18163, 18122, // 7C80 - 7CBF
                19002,  9717,  7422,  7277, 19577, 19002, 19002, 18264, // 7CC0 - 7CFF
                19569, 19554, 19546, 19534, 19495, 17873, 19432, 18082, // 7D00 - 7D3F
                 9621, 19002, 19423,  7199, 14773, 19407, 17973, 17934, // 7D40 - 7D7F
                19002,  7096,  4283,  1642, 19374,  1608, 14268, 19366, // 7D80 - 7DBF
                18042,  9603,  7001, 19334, 19295, 19287,  1502,  4292, // 7DC0 - 7DFF
                19002, 17831, 10400, 13460, 14156, 13245, 14052,  9546, // 7E00 - 7E3F
                13534, 19002,  4339,  6913, 13502, 17552, 19278, 17465, // 7E40 - 7E7F
                 9472,  1434, 13108, 19248, 19002, 19002, 19002, 19002, // 7E80 - 7EBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 7EC0 - 7EFF
                19002, 19002, 19002, 19002, 19002, 19002,  6866, 19236, // 7F00 - 7F3F
                19002,  1476, 19221, 19002, 19002,  9409, 19213, 13316, // 7F40 - 7F7F
                 7182, 19197,  1325,  9270,  1239, 19151,  4020, 19131, // 7F80 - 7FBF
                17406,  9230,  9127, 17757, 19092, 17387, 19083, 13072, // 7FC0 - 7FFF
                19028, 12787, 19020, 19011, 19002,  6020, 16092,  5052, // 8000 - 803F
                12016,  9026,  6802, 18975, 19002, 17696, 18967, 19326, // 8040 - 807F
                 1098, 17251,  6534, 15077, 12686, 17211, 17190,  8918, // 8080 - 80BF
                16636,  1071, 19002,  8863, 12307, 19002, 17116, 18952, // 80C0 - 80FF
                 8771, 18940, 19002, 18921, 16641, 12152, 19002, 17077, // 8100 - 813F
                19002, 11472, 18878, 19002,  2249, 11329, 18867, 18859, // 8140 - 817F
                18850, 18842, 19002,  8646, 18807, 18798, 11338,  8611, // 8180 - 81BF
                18760, 11824, 19002, 18752,  9703, 18697, 15089,  8534, // 81C0 - 81FF
                19724, 18679,  8495, 15094, 12067,  8453, 19526, 11794, // 8200 - 823F
                18671, 19002, 19002, 11589,   898,  6434, 17018, 18647, // 8240 - 827F
                19002,  8668,  8355, 16995,  3321, 11407, 16980, 18639, // 8280 - 82BF
                19002, 19002,  5503, 14645, 16882, 19002, 11006,  3021, // 82C0 - 82FF
                10831, 16793, 17442, 19002, 19002, 18631, 10610, 18620, // 8300 - 833F
                18612, 16735, 10157, 19002, 19002, 19002,  3366, 13953, // 8340 - 837F
                19002,  9945,  6334, 18596,  3840, 16584, 19002, 19316, // 8380 - 83BF
                16498,  9870, 10250,   722, 19002,  9825, 18582,  9651, // 83C0 - 83FF
                13581,  8274, 19002, 19002, 19002,  4353,  9321, 18567, // 8400 - 843F
                19002,  9163, 17425, 13586, 16330, 10083, 18898,  8200, // 8440 - 847F
                19002, 19002, 18539,  9097,  8958, 19002,  8105, 18531, // 8480 - 84BF
                18507, 16292,  3958,  8079, 19002,   815, 20488, 19062, // 84C0 - 84FF
                19002, 19002,  8831, 18456, 16234, 18339, 19002, 19947, // 8500 - 853F
                 3910, 16201,  9422, 16169, 11940, 18423, 19002, 19002, // 8540 - 857F
                 4533, 17368, 15992, 11954,  6145, 18415, 18406,  7989, // 8580 - 85BF
                15925,  7849,  1675, 12194, 19603, 15691, 17359,  7963, // 85C0 - 85FF
                 7922,  7721,  5938,  7666, 19002, 11613, 19002, 17101, // 8600 - 863F
                19002,  5808, 18391, 11968, 17062, 19002, 19002,  3464, // 8640 - 867F
                19002,  3603,  9243, 19002,  8991,  7392, 19002, 19002, // 8680 - 86BF
                16924,  8996,  8732, 15401,  9731, 19906, 19002,  5680, // 86C0 - 86FF
                18356, 18347, 19002, 18307, 19002, 19002, 19002, 19002, // 8700 - 873F
                19002,  5624, 10852, 16867,  5513, 18298,  4034, 18289, // 8740 - 877F
                 7612, 19800, 19002, 16858,  7572, 19002,  8880,  7500, // 8780 - 87BF
                19040, 19002, 19002, 19002, 18280,  3854,  7464, 15322, // 87C0 - 87FF
                15185, 19002, 19002, 16849,  7291, 19002, 15207,  8796, // 8800 - 883F
                18226,  9880,  7221, 15165, 15109, 18213, 18205,  5362, // 8840 - 887F
                15024, 18190,  3413,  5316, 19002, 15389, 18549, 19002, // 8880 - 88BF
                14942, 16823,  4870, 14897,  7256, 18181,  9388, 18156, // 88C0 - 88FF
                16752, 19002, 18106, 18072, 14278,  7110,  5254, 18059, // 8900 - 893F
                14751, 19002, 19002, 16613,  7514,  7031,  6987, 16604, // 8940 - 897F
                 6949, 14873,  8418, 19002,  6900,  3257,  3300,  6935, // 8980 - 89BF
                18050, 19002,  6832, 19002, 14845, 19002, 16424, 18041, // 89C0 - 89FF
                18028, 17997,  8378, 17989,  7820,  6752, 14387,  2094, // 8A00 - 8A3F
                19002, 19002, 17966,  7442, 17942, 14345, 17927, 14258, // 8A40 - 8A7F
                17416, 16371,  7359, 17911, 17881, 17843, 17820, 14243, // 8A80 - 8ABF
                 6738, 14330,  6608,  6652, 14222,  6567, 14197, 17795, // 8AC0 - 8AFF
                17773,  5228, 17765, 14149, 17756, 17734,  6657, 14067, // 8B00 - 8B3F
                14042,  6856, 19002, 17712,  5081,  7310, 17704, 12696, // 8B40 - 8B7F
                17695,  6472, 17679,  6448, 19002, 19002, 19002, 19002, // 8B80 - 8BBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 8BC0 - 8BFF
                19002, 19002, 19002, 19002, 19002, 19002, 16523, 16514, // 8C00 - 8C3F
                13834, 17627, 18888,  6375, 13524,  6356, 19002, 13491, // 8C40 - 8C7F
                 6251,  6203, 19002, 18707, 17619, 17611, 17603, 17595, // 8C80 - 8CBF
                17587, 17541, 13406,  6096, 17514,  6071, 14761,  6409, // 8CC0 - 8CFF
                14714, 17458,  2988, 19002, 19002, 19002, 19002, 19002, // 8D00 - 8D3F
                19002, 19002, 19002, 19002, 11708, 14030, 17450, 19002, // 8D40 - 8D7F
                19981,  5975, 19002, 13380, 13903, 17441,  5825,  5952, // 8D80 - 8DBF
                 5062, 13880, 19002, 13827, 13300, 17433,  5209, 19002, // 8DC0 - 8DFF
                19002,  5904, 17424,  4994, 19002,  5794, 17399, 19002, // 8E00 - 8E3F
                 2891, 17376, 19002,  6524, 17367, 19002,  6486, 19002, // 8E40 - 8E7F
                13150,  5763, 19002, 19002, 19002,  5656, 19002, 19002, // 8E80 - 8EBF
                17358,  5588,  5561, 16404, 19002,  5697, 19002, 17342, // 8EC0 - 8EFF
                 5707, 13062,  5536, 13794,  4931, 12989,  5637, 17334, // 8F00 - 8F3F
                14612, 12942, 19002, 18689,  5401, 19002, 19002, 19002, // 8F40 - 8F7F
                19002, 19002, 19002, 13693, 13698, 17326, 17314, 19002, // 8F80 - 8FBF
                 5376,  4835, 12833, 19002,  4772,  2723, 17306, 12780, // 8FC0 - 8FFF
                17290, 17282, 17274, 12737, 17235,  4611, 12679, 17219, // 9000 - 903F
                12645,  5294, 17204, 12550, 17178, 12356,  5280, 17170, // 9040 - 907F
                17162, 17154, 12300, 12227,  5167, 17146, 17109, 17100, // 9080 - 90BF
                12142,  5107, 19002,  4543, 12095, 17070, 11807, 19896, // 90C0 - 90FF
                 5024, 19002,  4945, 12057, 16395, 10171,  4810, 19002, // 9100 - 913F
                19002, 11999,  4722, 19002,  4661, 11896, 16182, 17061, // 9140 - 917F
                16277, 11787,  4565, 17637, 19002, 13670, 11582, 19002, // 9180 - 91BF
                17053, 11505, 17045, 17011,  2928,  4474, 15938, 19002, // 91C0 - 91FF
                19002,  7405, 17003,  4414, 19002, 19002,  5121,  1846, // 9200 - 923F
                16988, 11397, 16268, 13557,  4444, 19002, 19002, 19002, // 9240 - 927F
                16973, 19002, 11366, 16955, 19002, 19002,  4762,  5493, // 9280 - 92BF
                19002, 16965,  2534, 19002, 16942,  4400, 19002, 16932, // 92C0 - 92FF
                10924, 19002, 16923, 16915, 16898, 16890, 19002, 19002, // 9300 - 933F
                19002, 16875,  4278,  5184, 19002,  4374, 15175, 12895, // 9340 - 937F
                19002, 12011, 16770,  4310,  4984,  3827, 16866, 19002, // 9380 - 93BF
                19823, 19002, 10996,  4234, 10948, 19002,  4208, 19002, // 93C0 - 93FF
                11467, 19002, 19002, 16857, 15124, 20659, 14956, 16848, // 9400 - 943F
                13576, 19002, 10910, 20394, 19002, 19002, 19002, 13505, // 9440 - 947F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 9480 - 94BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 94C0 - 94FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 9500 - 953F
                19002, 19002, 19002, 19002, 19002, 19002, 16259, 19002, // 9540 - 957F
                16831, 10873, 10814, 16822, 20404, 16786,  5424, 12760, // 9580 - 95BF
                16250,  4264, 10020, 11935, 10683, 19002, 19002, 19002, // 95C0 - 95FF
                19002, 19002, 19002,  8791, 10600,  2444,  4186, 12765, // 9600 - 963F
                16760, 12730, 16751,  1213,  2397,  4124, 16743, 16728, // 9640 - 967F
                13319,  4056, 15158, 16703,  4110, 16685,  4959, 12424, // 9680 - 96BF
                16677, 10422, 19154,  4915,  3994, 16669, 16661, 10310, // 96C0 - 96FF
                16653, 19002, 10273, 10215, 16219, 19002, 16621, 10150, // 9700 - 973F
                 3817, 16612, 10008,  8563,  4597,  9938,  8481,  3799, // 9740 - 977F
                19002, 12434, 19002, 19002, 16603, 16577, 19002, 19002, // 9780 - 97BF
                 4156,    20, 20642,  8413, 19002, 19002,  3570, 14290, // 97C0 - 97FF
                16553, 16539, 16531, 16522, 19002, 19095, 16513, 16482, // 9800 - 983F
                 4138,  7644,  7354, 16458, 18870, 20363, 19002, 19002, // 9840 - 987F
                19002, 19002, 19002, 19002, 19002, 16450,  9860, 19002, // 9880 - 98BF
                 6045, 19002, 19002,  6001,  9814, 12070,  2983,  5845, // 98C0 - 98FF
                 1760,  9688, 16437, 16412, 16403, 16394, 19002, 19002, // 9900 - 993F
                11797,  9644,  9491, 19002, 19002, 19002, 19002, 19002, // 9940 - 997F
                19002, 19002,  4093,  4346, 11592, 16359,  9448,  9311, // 9980 - 99BF
                 9153, 19002, 16323,  9134, 19002, 11009,  9087, 16123, // 99C0 - 99FF
                 9045, 16315, 19002,  8948, 19002, 11739, 16307, 19002, // 9A00 - 9A3F
                16285, 16796, 18623,  3740,  3715,  8929, 19002, 19002, // 9A40 - 9A7F
                19002, 19002, 19002, 19002, 19002, 16276, 19002, 16267, // 9A80 - 9ABF
                19002, 19002, 11749, 16258,  9324,  3971, 19002, 19002, // 9AC0 - 9AFF
                19002, 19002, 19002,  3678, 16007,  3553,  3948, 14025, // 9B00 - 9B3F
                 8824, 15847, 13898,  3505, 19002, 15715, 19002, 19002, // 9B40 - 9B7F
                19002,  3769,  8699, 15570, 19002, 11754, 19002, 19002, // 9B80 - 9BBF
                19002,  8653,  3630, 19002,  5820, 16249, 19002, 19002, // 9BC0 - 9BFF
                19002,  9166, 16227, 19002,  8961, 19002,  3481,  1584, // 9C00 - 9C3F
                15524,  3899, 15458, 19002, 19002, 19002, 19002, 19002, // 9C40 - 9C7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 9C80 - 9CBF
                19002, 19002, 19002, 19002,  3961,  8230, 11681, 19002, // 9CC0 - 9CFF
                19002,  8211, 19002,  1263,  3519, 16218, 19002,   424, // 9D00 - 9D3F
                19002, 19002,  8152, 16172, 16190, 13875,  3351, 19002, // 9D40 - 9D7F
                19002, 19002, 19002, 19002, 19002,  8137, 13822, 19002, // 9D80 - 9DBF
                13552, 19002, 15427, 19002, 19002, 19002,  3191, 16152, // 9DC0 - 9DFF
                19002, 19002, 19002,  3138, 19002, 19002, 19002, 19002, // 9E00 - 9E3F
                19002, 19002, 19002, 19002, 19002, 19002, 15928,  8122, // 9E40 - 9E7F
                19002, 19002,  2017,  3467,  7395, 19002, 13373,   434, // 9E80 - 9EBF
                19619,  7259,  8008, 16131, 19002, 16122,  4757, 19002, // 9EC0 - 9EFF
                15140, 16015, 20575, 19002, 16006, 19002, 19002, 19511, // 9F00 - 9F3F
                19002,  3112,  3043, 14882,  7937,  1933, 14822, 19002, // 9F40 - 9F7F
                19002, 18075, 15985,  5179, 19002, 19002, 19002, 19002, // 9F80 - 9FBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // 9FC0 - 9FFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A000 - A03F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A040 - A07F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A080 - A0BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A0C0 - A0FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A100 - A13F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A140 - A17F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A180 - A1BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A1C0 - A1FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A200 - A23F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A240 - A27F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A280 - A2BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A2C0 - A2FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A300 - A33F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A340 - A37F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A380 - A3BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A3C0 - A3FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A400 - A43F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A440 - A47F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A480 - A4BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A4C0 - A4FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A500 - A53F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A540 - A57F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A580 - A5BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A5C0 - A5FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A600 - A63F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A640 - A67F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A680 - A6BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A6C0 - A6FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A700 - A73F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A740 - A77F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A780 - A7BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A7C0 - A7FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A800 - A83F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A840 - A87F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A880 - A8BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A8C0 - A8FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A900 - A93F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A940 - A97F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A980 - A9BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // A9C0 - A9FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // AA00 - AA3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // AA40 - AA7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // AA80 - AABF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // AAC0 - AAFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // AB00 - AB3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // AB40 - AB7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // AB80 - ABBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // ABC0 - ABFF
                15977, 15914, 15906,  7842, 15892,  3157, 15869, 15855, // AC00 - AC3F
                15846, 20531, 12725, 15832, 19002, 19002, 15824, 15812, // AC40 - AC7F
                15780,  7732, 15757, 12616, 15731, 15723, 15714, 15684, // AC80 - ACBF
                 7683, 15670,  6903, 19002, 15662, 15654, 15628, 19811, // ACC0 - ACFF
                15601, 12280,  3616, 15578, 15569,  3457, 12135, 15555, // AD00 - AD3F
                 3124,  7538, 15532, 15523,  7475, 12050, 15480, 20510, // AD40 - AD7F
                 7381, 15466, 15457, 11780, 11734, 19002, 14790, 19002, // AD80 - ADBF
                15435, 15426, 15409, 11676, 15374, 19002, 19002, 15366, // ADC0 - ADFF
                15339, 15315,  1258, 19002, 19002, 19002, 15307, 15295, // AE00 - AE3F
                15279,  3010, 15253,   989, 15200, 15148, 15139, 15102, // AE40 - AE7F
                20626, 20505, 19002, 19002, 19002, 19002, 19002, 11312, // AE80 - AEBF
                15036, 11247, 15013, 14971, 19002, 14935, 20246, 14905, // AEC0 - AEFF
                14552, 14890, 14881, 19002, 19002, 20167, 14862,  2801, // AF00 - AF3F
                 7244, 14830, 14821, 10807,  9662, 19002, 19002,  7066, // AF40 - AF7F
                14798, 14789, 14744,  4686, 19002, 19002, 19002, 14730, // AF80 - AFBF
                14722, 14653,  9921,  4523, 19002, 14596, 14560, 14551, // AFC0 - AFFF
                14537,  9721, 14523, 19499, 19002, 14514, 19002, 19002, // B000 - B03F
                18086, 14490, 18098,  6942, 19002, 19002, 19002, 19378, // B040 - B07F
                14476, 17835,  6889, 14468, 14456, 14448,  3250, 14434, // B080 - B0BF
                 9476, 14403, 14370, 14361, 14338, 19002, 19002, 19002, // B0C0 - B0FF
                19002, 14319, 14298, 14251, 20351, 14230, 19240, 14205, // B100 - B13F
                14184, 14175, 14142, 14128, 14110, 19002, 19002, 14096, // B140 - B17F
                14078, 14060,  2973, 14015, 19002, 13920, 19002, 19002, // B180 - B1BF
                19002,  4024, 13888,  9030, 19002, 13865, 13855,  6849, // B1C0 - B1FF
                 8922, 13812, 18944,  6788, 13802, 19002,  8499,  8359, // B200 - B23F
                19002, 19002, 19002, 13779, 13769, 13740, 10161,  3844, // B240 - B27F
                 8204,  6759, 13678, 13651, 13635,  2917, 13613, 19002, // B280 - B2BF
                19002, 13542, 13513, 13484,  2855, 13476, 11286, 13468, // B2C0 - B2FF
                13437, 13419, 13399,  8109, 19002, 19002, 19002, 19002, // B300 - B33F
                19002, 19002,  8083, 13391,  7993,  6639, 13363, 13345, // B340 - B37F
                13293, 15695, 13279, 19002, 13267, 13253, 19002, 19002, // B380 - B3BF
                 7725, 13232, 13224,  6517, 13210, 13192, 19002,  7670, // B3C0 - B3FF
                19002, 19002, 13182, 13168, 13158, 13138,  7616, 19002, // B400 - B43F
                19002, 19002, 13124, 13093, 13055,  7576, 19002, 19002, // B440 - B47F
                13045, 13019, 19002, 14261,  7504, 12997, 14070,  6386, // B480 - B4BF
                12976, 12958, 12935,  7468, 12919, 15028,  6119, 12881, // B4C0 - B4FF
                19002, 19002, 14946, 12859,  7114, 12847, 12820, 12801, // B500 - B53F
                12773, 17564, 12745,  7035, 12711, 19002, 19002, 19002, // B540 - B57F
                19002, 19002, 19002, 19002, 12666, 12653, 12638, 17483, // B580 - B5BF
                12624,  6991, 12602, 12584, 19002,  3261, 19002, 19002, // B5C0 - B5FF
                19002, 19002, 12537, 12511, 14045,  3304,  6836, 19002, // B600 - B63F
                19002, 12470, 19002, 19002, 17847, 12442, 19002, 19002, // B640 - B67F
                19002, 19002, 19002, 17824, 12413, 20251,  6078, 19002, // B680 - B6BF
                19002, 19002,  6476, 19002, 19002, 19002, 12383, 12364, // B6C0 - B6FF
                12349, 19002, 19002, 19002, 19002, 12341, 12322, 12288, // B700 - B73F
                 6452, 12270,  6379, 19002, 12256, 12238, 12220, 13495, // B740 - B77F
                12168,  6255, 12160, 12125, 12106, 12088, 17545, 12078, // B780 - B7BF
                14515,  5986, 19002, 19002, 19002,  5979, 12040,  5798, // B7C0 - B7FF
                12032, 11986, 11976, 11889, 17380, 11859,  5565, 11839, // B800 - B83F
                11770, 19002,  5919,  5405, 11724, 17318,  5880, 11666, // B840 - B87F
                19002, 13527, 19002, 19002, 19002, 11656, 11638, 11628, // B880 - B8BF
                11575,  5111, 11561, 13383,  5770, 11539, 11513, 11498, // B8C0 - B8FF
                 5028, 19002, 19002, 11488, 11453, 19002, 19002,  4814, // B900 - B93F
                11439,  4726,  2831, 11425, 11415, 11390,  4665, 11353, // B940 - B97F
                 4569,  5595, 19002, 19002, 19002,  4478, 11302,  2538, // B980 - B9BF
                 5543, 11294, 11271, 11255,  5486, 11237,  4404, 11225, // B9C0 - B9FF
                11188, 11178, 13065, 19002, 19002, 19002, 19002, 11164, // BA00 - BA3F
                11152, 11136, 20256, 11114,  4314, 11094, 11045, 11024, // BA40 - BA7F
                20199, 10914, 19002, 19002, 19002, 10978, 10937, 10903, // BA80 - BABF
                 4268, 10893, 19002, 10881, 19002, 19002, 19002, 10687, // BAC0 - BAFF
                10860, 12230,  2787, 10797, 10787,  5412,  4190, 10747, // BB00 - BB3F
                 4128,  5383, 10728, 10709,  5347, 16689, 19002, 19002, // BB40 - BB7F
                19002, 10665, 10655, 19002, 16625, 10633,  4601, 19002, // BB80 - BBBF
                10582, 10564, 10532, 19002, 19002, 19002, 19002, 10524, // BBC0 - BBFF
                10512, 10504,  2730, 10496,  3803,  4952, 10474, 10448, // BC00 - BC3F
                10415,  4908, 10377, 12145, 19002, 19002, 19002, 19002, // BC40 - BC7F
                 9818, 10353,  8933,  4821, 10345, 10327, 10303,  3682, // BC80 - BCBF
                10289, 12098, 10266, 10231, 19002, 19002,  3557, 10202, // BCC0 - BCFF
                 3509,  4672, 10133, 19002,  3485,  3355, 19002, 19002, // BD00 - BD3F
                10123, 10109, 10091, 10066,  3195, 10056, 19002, 19002, // BD40 - BD7F
                10048, 10036, 10001,  4584, 16156, 19002,  9979,  9961, // BD80 - BDBF
                19002, 19002,  3047,  9911, 19002,  4509,  9843,  9833, // BDC0 - BDFF
                 9807, 15918,  9793, 15816, 19002, 19002, 19002, 19002, // BE00 - BE3F
                 7385,  9760, 15299,  4485,  9711,  9677,  9637, 10989, // BE40 - BE7F
                 9615, 14460,  9593,  9581, 19002,  9526, 19002, 19002, // BE80 - BEBF
                19002, 19002,  9518,  9499,  9484, 14082, 19002, 19002, // BEC0 - BEFF
                 4329,  9462, 19002,  9441, 19002, 19002, 19002, 19002, // BF00 - BF3F
                 9403,  9358,  9304, 19002, 19002, 19002, 19002, 19002, // BF40 - BF7F
                19002, 19002, 13655, 19002, 19002, 19002,  9286, 19002, // BF80 - BFBF
                12060, 13517,  9264, 12851,  4079, 19002, 19002, 19002, // BFC0 - BFFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 11275, // C000 - C03F
                19002, 19002,  4010,  9224,  9214,  9142, 19002, 19002, // C040 - C07F
                19002, 19002,  9121,  9111,  9080, 11229,  9056, 11156, // C080 - C0BF
                 9038,  9020,  8976,  8941, 10941,  8912, 10516,  3937, // C0C0 - C0FF
                 8857,  8839,  8817,  4825,  8809, 10040,  8779,  8765, // C100 - C13F
                 8710,  8692,  9681,  8640,  7949,  8619,  8605,  8586, // C140 - C17F
                11400,  7861,  8551,  6724,  3892,  8528,  8518,  5496, // C180 - C1BF
                 6245,  8489,  6057,  8467,  8447,  8399,  8386,  5873, // C1C0 - C1FF
                 8349,  5463,  3869,  8341,  8316,  8308,  3834, 19002, // C200 - C23F
                19002,  8290,  8268,  8242,  8223,  2813,  8194,  2353, // C240 - C27F
                 3755,  8182,  8164,  8145,  1736,  8130,  1344,  3722, // C280 - C2BF
                19002, 19002, 19002,  2495,  8099, 19143,  3689,  8073, // C2C0 - C2FF
                 8022,  8001, 20109,  7983,  2380,  7971, 19002, 19002, // C300 - C33F
                10999, 19002, 19002, 19002, 19002,  7957,  7945,  7930, // C340 - C37F
                 9345,  7916, 19002, 19002, 19002, 19002, 19002, 19002, // C380 - C3BF
                 7906, 19002, 19002,  7898,  7857,  7835, 15234,  7798, // C3C0 - C3FF
                19002,  7780,  7762, 19002, 11949, 11963,  7715,  9005, // C400 - C43F
                19002,  7697, 19002, 19002,  9931,  7660, 11071,  3593, // C440 - C47F
                 7630, 19002,  9780,  8430, 19002, 19002, 19002,  7606, // C480 - C4BF
                19002, 19002, 19002, 19002, 19002,  3446,  7566,  7558, // C4C0 - C4FF
                 7527,  6681,  7494,  7300, 19002,  7458,  7420,  7374, // C500 - C53F
                20219,  7342, 20083,  7318,  7285,  7275,  7237, 18115, // C540 - C57F
                 7229, 14854,  3428,  7215,  7197,  3403, 17893,  7160, // C580 - C5BF
                 4881,  7122,  7104,  7094,  7059, 17663,  7051, 13912, // C5C0 - C5FF
                 7043,  7025,  6999,  6981,  5834,  6965,  6957,  3285, // C600 - C63F
                 6929,  6911,  6882, 13309,  6864,  5218,  3239,  6826, // C640 - C67F
                 6800,  6781, 17030,  6746, 15245,  2084,  6732,  6720, // C680 - C6BF
                 6632, 17244,  6602,  5193,  6590,  6561,  6532,  6510, // C6C0 - C6FF
                20668,  6466, 16840,  3094,  6442,  6432,  6424, 15613, // C700 - C73F
                 6369,  2765,  3076,  6350,  6332,  6303,  4003,  6287, // C740 - C77F
                10739,  6271,  6263,  6241,  6217, 15054,  6197, 10319, // C780 - C7BF
                 6185,  6162,  6143,  6112, 14989,  6090, 19002, 19002, // C7C0 - C7FF
                 6065,  6053,  6009, 16491,  5969,  1769,  3054,  5946, // C800 - C83F
                 5936,  5912, 19353, 19002, 19002, 19002,  5898,  5869, // C840 - C87F
                 5861, 10676, 19180,  5427,  2943,  5806, 19002, 10824, // C880 - C8BF
                13455,  5788, 17533,  2881,  5757, 19002, 10603, 20584, // C8C0 - C8FF
                 5726, 15418,  2820,  5678, 19002, 15072, 11324, 19002, // C900 - C93F
                19002, 19002,  5650,  5622,  5573,  9698,  5555, 14640, // C940 - C97F
                19002,  5530,  5511,  5479, 19002, 19002, 19002, 19002, // C980 - C9BF
                 5471,  5459,  5443,  2780,  5435,  5928,  5391,  5370, // C9C0 - C9FF
                 5360,  5340, 13948,  5314, 19002,  2713, 19002, 19002, // CA00 - CA3F
                19002, 13992,  5288,  9993,  5270,  5252, 19002,  9863, // CA40 - CA7F
                10541, 19002, 19002,  5226, 19002, 19002, 19002,  9372, // CA80 - CABF
                 5161,  4494,  2660,  5097,  5079,  3294,  8875, 19002, // CAC0 - CAFF
                19002,  5060,  5018,  4992,  4967, 19002, 19002, 19002, // CB00 - CB3F
                 2641,  4939,  4929,  4901, 15384, 19002, 19002,  4889, // CB40 - CB7F
                19002, 19002, 19002,  9383, 19002, 19002, 19002,  4833, // CB80 - CBBF
                19002, 19002,  5582, 19002,  2669,  2619, 19002, 19002, // CBC0 - CBFF
                19002, 14380,  4804,  7172,  2601,  4788,  4770,  4734, // CC00 - CC3F
                 6546,  4716,  6135,  4651,  4635,  4609,  4577, 19002, // CC40 - CC7F
                19002, 19002, 19002,  4559,  4541,  4502,  2066,  4468, // CC80 - CCBF
                 5718,  1836,  4438, 19002,  5204,  5692,  4412, 19002, // CCC0 - CCFF
                 2524,  4394,  4372,  4322, 13789,  4304, 19002,  2502, // CD00 - CD3F
                19002, 19002, 19002,  8741,  4258,  6773,  2434,  4232, // CD40 - CD7F
                19002,  4206,  8750,  4180,  8439,  2387,  4154, 19002, // CD80 - CDBF
                13688,  6690,  4136, 19002, 19002,  4118,  4091,  4072, // CDC0 - CDFF
                13665,  4050,  6699,  2294,  3988,  3969,  3930, 19002, // CE00 - CE3F
                19002, 19002, 19002,  3922,  3881,  3862,  5607,  3811, // CE40 - CE7F
                 5306,  2272,  3793,  3767,  3748, 20010, 19002, 19002, // CE80 - CEBF
                 2219, 19002, 19002, 19002,  1396,  3734, 19877,  3705, // CEC0 - CEFF
                 3672,  3628,  3586,  1518,  3547, 17266,  3535,  3517, // CF00 - CF3F
                19002, 19002,  4746,  3499,  4386,  2155,  3475,  3436, // CF40 - CF7F
                 3421, 12755, 19002, 19002,  2118,  3393,  3374, 19002, // CF80 - CFBF
                 4105, 19002, 19002, 19002,  3345,  3275,  3232, 20637, // CFC0 - CFFF
                 3185, 19002,  2073,  3165, 19002,  9314,  3642,  3132, // D000 - D03F
                17812,  2007,  3106,  3084,  3066, 18322,  3037, 19992, // D040 - D07F
                 1923, 19002, 19002, 19002, 15962,  3004, 16814,  1893, // D080 - D0BF
                 2967,  2955,  2936,  1125,  2911, 11648,  2899,  2871, // D0C0 - D0FF
                19002,  9156, 19002, 19002, 19002, 19002,  2849,  2809, // D100 - D13F
                 2773, 14998,  2759,  9573,  1871,  2738, 19002, 17646, // D140 - D17F
                19614,  2703, 19002, 19002,  2688,  2677,  2653,  2744, // D180 - D1BF
                 2631, 19002, 19002,  2609, 19002, 19002,  8056,  2591, // D1C0 - D1FF
                14362,  1825,  2580, 19002, 19002, 20375,  2546,  8065, // D200 - D23F
                 1743,  2514, 19002, 20194, 20104, 19002, 19002, 19002, // D240 - D27F
                 2484,  2473,  2427,  5130,  2369,  5139,  1720,  2361, // D280 - D2BF
                 2349,  2336,  5148,  2321, 20054, 19002,  2306,  2284, // D2C0 - D2FF
                 2265, 11916,  2231, 13761,  2212,  2194,  2183,  2148, // D300 - D33F
                11603, 19002, 19002, 19002, 19002, 19002, 19002, 10842, // D340 - D37F
                 2130, 19189,  2107,  2045,  2025,  2000,  9335,  1982, // D380 - D3BF
                10143,  1947,  1913,  1883,  1615,  9853,  1854, 18744, // D3C0 - D3FF
                 1638,  1815, 19002,  9090, 19002, 19002, 19002, 19002, // D400 - D43F
                 1800, 19002, 19002, 18250,  1785,  9048, 19002,  1777, // D440 - D47F
                 1732,  1713, 18259, 19002, 19002,  1604, 19002, 19002, // D480 - D4BF
                19002, 18167,  1690, 19538, 19002,  1657,  1628,  1597, // D4C0 - D4FF
                 4287,  1561,  9607, 19002, 19002, 19002, 19002, 19299, // D500 - D53F
                 1546,  4296,  1498,  1531,  1491,  1465, 14160,  1442, // D540 - D57F
                17391,  1430,  1419, 19002,  8951, 19002, 19002, 19002, // D580 - D5BF
                19002,  1352,  1340,  1307, 18925,  1284, 16645,  1472, // D5C0 - D5FF
                 1221,  1190,  1170, 11333,  1148,  3951, 19002,  1106, // D600 - D63F
                 1087,  1060,  1314,  1045, 14176,  1321,  1027, 19002, // D640 - D67F
                10593, 18811,  1012,  8702,  1235,   997,   974,  1177, // D680 - D6BF
                13957,   956,  1162,  1094,   922,   884,   854, 16588, // D6C0 - D6FF
                  839, 19002,  1067,   797,   779,   763, 13590,   745, // D700 - D73F
                 3914,   894,   737,   718,   702,   861,   684,  9426, // D740 - D77F
                  811,   669,   650,   634, 19002, 19002, 19002, 19002, // D780 - D7BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // D7C0 - D7FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // D800 - D83F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // D840 - D87F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // D880 - D8BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // D8C0 - D8FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // D900 - D93F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // D940 - D97F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // D980 - D9BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // D9C0 - D9FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DA00 - DA3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DA40 - DA7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DA80 - DABF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DAC0 - DAFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DB00 - DB3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DB40 - DB7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DB80 - DBBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DBC0 - DBFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DC00 - DC3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DC40 - DC7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DC80 - DCBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DCC0 - DCFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DD00 - DD3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DD40 - DD7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DD80 - DDBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DDC0 - DDFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DE00 - DE3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DE40 - DE7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DE80 - DEBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DEC0 - DEFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DF00 - DF3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DF40 - DF7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DF80 - DFBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // DFC0 - DFFF
                  626,   618,   610,   602,   594,   586,   578,   570, // E000 - E03F
                  562,   554,   546,   538,   530,   522,   514,   506, // E040 - E07F
                  498,   490,   482,   474,   466,   458,   450,   406, // E080 - E0BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E0C0 - E0FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E100 - E13F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E140 - E17F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E180 - E1BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E1C0 - E1FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E200 - E23F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E240 - E27F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E280 - E2BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E2C0 - E2FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E300 - E33F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E340 - E37F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E380 - E3BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E3C0 - E3FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E400 - E43F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E440 - E47F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E480 - E4BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E4C0 - E4FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E500 - E53F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E540 - E57F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E580 - E5BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E5C0 - E5FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E600 - E63F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E640 - E67F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E680 - E6BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E6C0 - E6FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E700 - E73F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E740 - E77F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E780 - E7BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E7C0 - E7FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E800 - E83F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E840 - E87F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E880 - E8BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E8C0 - E8FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E900 - E93F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E940 - E97F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E980 - E9BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // E9C0 - E9FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EA00 - EA3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EA40 - EA7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EA80 - EABF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EAC0 - EAFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EB00 - EB3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EB40 - EB7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EB80 - EBBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EBC0 - EBFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EC00 - EC3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EC40 - EC7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EC80 - ECBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // ECC0 - ECFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // ED00 - ED3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // ED40 - ED7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // ED80 - EDBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EDC0 - EDFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EE00 - EE3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EE40 - EE7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EE80 - EEBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EEC0 - EEFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EF00 - EF3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EF40 - EF7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EF80 - EFBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // EFC0 - EFFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F000 - F03F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F040 - F07F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F080 - F0BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F0C0 - F0FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F100 - F13F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F140 - F17F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F180 - F1BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F1C0 - F1FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F200 - F23F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F240 - F27F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F280 - F2BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F2C0 - F2FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F300 - F33F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F340 - F37F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F380 - F3BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F3C0 - F3FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F400 - F43F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F440 - F47F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F480 - F4BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F4C0 - F4FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F500 - F53F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F540 - F57F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F580 - F5BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F5C0 - F5FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F600 - F63F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F640 - F67F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F680 - F6BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F6C0 - F6FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F700 - F73F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F740 - F77F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F780 - F7BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F7C0 - F7FF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F800 - F83F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F840 - F87F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F880 - F8BF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // F8C0 - F8FF
                  398,   390,   382,   374,   366,   358,   350,   342, // F900 - F93F
                  334,   326,   318,   310,   302,   294,   286,   278, // F940 - F97F
                  270,   262,   254,   246,   238,   230,   222,   214, // F980 - F9BF
                  206,   198,   190,   182,   174,   166,   158,   150, // F9C0 - F9FF
                  142,   129, 19002, 19002, 19002, 19002, 19002, 19002, // FA00 - FA3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FA40 - FA7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FA80 - FABF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FAC0 - FAFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FB00 - FB3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FB40 - FB7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FB80 - FBBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FBC0 - FBFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FC00 - FC3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FC40 - FC7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FC80 - FCBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FCC0 - FCFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FD00 - FD3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FD40 - FD7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FD80 - FDBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FDC0 - FDFF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FE00 - FE3F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FE40 - FE7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FE80 - FEBF
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FEC0 - FEFF
                  789,   121,   113,   105,    97,    89,    81,    73, // FF00 - FF3F
                   65,    57,    49,    33, 19002, 19002, 19002, 19002, // FF40 - FF7F
                19002, 19002, 19002, 19002, 19002, 19002, 19002, 19002, // FF80 - FFBF
                19002, 19002, 19002, 19002,     0, 19002, 19002, 19002,
        };

        private final static String index2;
        private final static String index2a;
        static {
            index2 =
                "\uA1CB\uA1CC\uA1FE\uA3FE\u0000\uA1CD\uA3DC\u0000\uD7B9\u0000" + //     0 -     9
                "\uE9C3\u0000\uE8FD\uE8AF\u0000\u0000\uDFBB\u0000\u0000\uF3A5" + //    10 -    19
                "\u0000\u0000\u0000\uEADF\u0000\u0000\u0000\u0000\uE4EF\u0000" + //    20 -    29
                "\u0000\u0000\uE9B9\uA3F8\uA3F9\uA3FA\uA3FB\uA3FC\uA3FD\uA2A6" + //    30 -    39
                "\u0000\u0020\u0021\"\u0023\u0024\u0025\u0026\u0027\uA3F0" + //    40 -    49
                "\uA3F1\uA3F2\uA3F3\uA3F4\uA3F5\uA3F6\uA3F7\uA3E8\uA3E9\uA3EA" + //    50 -    59
                "\uA3EB\uA3EC\uA3ED\uA3EE\uA3EF\uA3E0\uA3E1\uA3E2\uA3E3\uA3E4" + //    60 -    69
                "\uA3E5\uA3E6\uA3E7\uA3D8\uA3D9\uA3DA\uA3DB\uA1AC\uA3DD\uA3DE" + //    70 -    79
                "\uA3DF\uA3D0\uA3D1\uA3D2\uA3D3\uA3D4\uA3D5\uA3D6\uA3D7\uA3C8" + //    80 -    89
                "\uA3C9\uA3CA\uA3CB\uA3CC\uA3CD\uA3CE\uA3CF\uA3C0\uA3C1\uA3C2" + //    90 -    99
                "\uA3C3\uA3C4\uA3C5\uA3C6\uA3C7\uA3B8\uA3B9\uA3BA\uA3BB\uA3BC" + //   100 -   109
                "\uA3BD\uA3BE\uA3BF\uA3B0\uA3B1\uA3B2\uA3B3\uA3B4\uA3B5\uA3B6" + //   110 -   119
                "\uA3B7\uA3A8\uA3A9\uA3AA\uA3AB\uA3AC\uA3AD\uA3AE\uA3AF\uFAA1" + //   120 -   129
                "\uFAA2\uFAE6\uFCA9\u0000\u0000\u0000\u0000\uD8D0\u0000\uF0C8" + //   130 -   139
                "\uD1A1\uD1A2\uF4EE\uF6F4\uF6F6\uF7B8\uF7C8\uF7D3\uF8DB\uF8F0" + //   140 -   149
                "\uEDA2\uEDA3\uEDEE\uEEDB\uF2BD\uF2FA\uF3B1\uF4A7\uECE1\uECE4" + //   150 -   159
                "\uECE7\uECE8\uECF7\uECF8\uECFA\uEDA1\uECC1\uECC5\uECC6\uECC9" + //   160 -   169
                "\uECCA\uECD5\uECDD\uECDE\uECAF\uECB0\uECB1\uECB2\uECB5\uECB8" + //   170 -   179
                "\uECBA\uECC0\uEBCF\uEBD0\uEBD1\uEBD2\uEBD8\uECA6\uECA7\uECAA" + //   180 -   189
                "\uEBBA\uEBBB\uEBBD\uEBC1\uEBC2\uEBC6\uEBC7\uEBCC\uEAF4\uEAF7" + //   190 -   199
                "\uEAFC\uEAFE\uEBA4\uEBA7\uEBA9\uEBAA\uE8FB\uE8FE\uE9A7\uE9AC" + //   200 -   209
                "\uE9CC\uE9F7\uEAC1\uEAE5\uE7E6\uE7F7\uE8E7\uE8E8\uE8F0\uE8F1" + //   210 -   219
                "\uE8F7\uE8F9\uE7C7\uE7CB\uE7CD\uE7CF\uE7D0\uE7D3\uE7DF\uE7E4" + //   220 -   229
                "\uE7A9\uE7AA\uE7AC\uE7AD\uE7B0\uE7BF\uE7C1\uE7C6\uE6F1\uE6F2" + //   230 -   239
                "\uE6F5\uE6F6\uE6F7\uE6F9\uE7A1\uE7A6\uE6E4\uE6E5\uE6E6\uE6E8" + //   240 -   249
                "\uE6EA\uE6EB\uE6EC\uE6EF\uE6C7\uE6CA\uE6D2\uE6D6\uE6D9\uE6DC" + //   250 -   259
                "\uE6DF\uE6E1\uE6B0\uE6B1\uE6B3\uE6B7\uE6B8\uE6BC\uE6C4\uE6C6" + //   260 -   269
                "\uE5FB\uE5FC\uE5FE\uE6A1\uE6A4\uE6A7\uE6AD\uE6AF\uE5BB\uE5BC" + //   270 -   279
                "\uE5C4\uE5CE\uE5D0\uE5D2\uE5D6\uE5FA\uE1ED\uE3F5\uE4A1\uE4A9" + //   280 -   289
                "\uE5AE\uE5B1\uE5B2\uE5B9\uDDF4\uDEFC\uDEFE\uDFB3\uDFE1\uDFE8" + //   290 -   299
                "\uE0F1\uE1AD\uD6CD\uD7CB\uD7E4\uDBC5\uDBE4\uDCA5\uDDA5\uDDD5" + //   300 -   309
                "\uD2F7\uD2F8\uD4E6\uD4FC\uD5A5\uD5AB\uD5AE\uD6B8\uD2EA\uD2EB" + //   310 -   319
                "\uD2F0\uD2F1\uD2F2\uD2F3\uD2F4\uD2F5\uD2E1\uD2E2\uD2E4\uD2E5" + //   320 -   329
                "\uD2E6\uD2E7\uD2E8\uD2E9\uD2D4\uD2D5\uD2D6\uD2D7\uD2D9\uD2DA" + //   330 -   339
                "\uD2DE\uD2DF\uD2CB\uD2CD\uD2CE\uD2CF\uD2D0\uD2D1\uD2D2\uD2D3" + //   340 -   349
                "\uD2C2\uD2C3\uD2C4\uD2C6\uD2C7\uD2C8\uD2C9\uD2CA\uD2A7\uD2A8" + //   350 -   359
                "\uD2A9\uD2AA\uD2AB\uD2AD\uD2B2\uD2BE\uD1F2\uD1F6\uD1FA\uD1FC" + //   360 -   369
                "\uD1FD\uD1FE\uD2A2\uD2A3\uD1E6\uD1E8\uD1E9\uD1EA\uD1EB\uD1ED" + //   370 -   379
                "\uD1EF\uD1F0\uD1DD\uD1DE\uD1DF\uD1E0\uD1E2\uD1E3\uD1E4\uD1E5" + //   380 -   389
                "\uD0B8\uD0D0\uD0DD\uD1D4\uD1D5\uD1D8\uD1DB\uD1DC\uCBD0\uCBD6" + //   390 -   399
                "\uCBE7\uCDCF\uCDE8\uCEAD\uCFFB\uD0A2\uFEFB\uFEFC\uFEFD\uFEFE" + //   400 -   409
                "\u0000\u0000\u0000\u0000\uEDD5\u0000\u0000\uD9BA\u0000\uE9DC" + //   410 -   419
                "\uD9C1\u0000\uF5F2\uE0C5\u0000\u0000\u0000\uFBF8\u0000\u0000" + //   420 -   429
                "\u0000\u0000\uCCF9\uCDB5\u0000\u0000\u0000\uD8AB\u0000\u0000" + //   430 -   439
                "\uFDCB\u0000\uA7BF\uA7C0\uA7C1\uA7C2\uA7C3\uA7C4\uA7C5\uA7C6" + //   440 -   449
                "\uFEF3\uFEF4\uFEF5\uFEF6\uFEF7\uFEF8\uFEF9\uFEFA\uFEEB\uFEEC" + //   450 -   459
                "\uFEED\uFEEE\uFEEF\uFEF0\uFEF1\uFEF2\uFEE3\uFEE4\uFEE5\uFEE6" + //   460 -   469
                "\uFEE7\uFEE8\uFEE9\uFEEA\uFEDB\uFEDC\uFEDD\uFEDE\uFEDF\uFEE0" + //   470 -   479
                "\uFEE1\uFEE2\uFED3\uFED4\uFED5\uFED6\uFED7\uFED8\uFED9\uFEDA" + //   480 -   489
                "\uFECB\uFECC\uFECD\uFECE\uFECF\uFED0\uFED1\uFED2\uFEC3\uFEC4" + //   490 -   499
                "\uFEC5\uFEC6\uFEC7\uFEC8\uFEC9\uFECA\uFEBB\uFEBC\uFEBD\uFEBE" + //   500 -   509
                "\uFEBF\uFEC0\uFEC1\uFEC2\uFEB3\uFEB4\uFEB5\uFEB6\uFEB7\uFEB8" + //   510 -   519
                "\uFEB9\uFEBA\uFEAB\uFEAC\uFEAD\uFEAE\uFEAF\uFEB0\uFEB1\uFEB2" + //   520 -   529
                "\uFEA3\uFEA4\uFEA5\uFEA6\uFEA7\uFEA8\uFEA9\uFEAA\uC9F9\uC9FA" + //   530 -   539
                "\uC9FB\uC9FC\uC9FD\uC9FE\uFEA1\uFEA2\uC9F1\uC9F2\uC9F3\uC9F4" + //   540 -   549
                "\uC9F5\uC9F6\uC9F7\uC9F8\uC9E9\uC9EA\uC9EB\uC9EC\uC9ED\uC9EE" + //   550 -   559
                "\uC9EF\uC9F0\uC9E1\uC9E2\uC9E3\uC9E4\uC9E5\uC9E6\uC9E7\uC9E8" + //   560 -   569
                "\uC9D9\uC9DA\uC9DB\uC9DC\uC9DD\uC9DE\uC9DF\uC9E0\uC9D1\uC9D2" + //   570 -   579
                "\uC9D3\uC9D4\uC9D5\uC9D6\uC9D7\uC9D8\uC9C9\uC9CA\uC9CB\uC9CC" + //   580 -   589
                "\uC9CD\uC9CE\uC9CF\uC9D0\uC9C1\uC9C2\uC9C3\uC9C4\uC9C5\uC9C6" + //   590 -   599
                "\uC9C7\uC9C8\uC9B9\uC9BA\uC9BB\uC9BC\uC9BD\uC9BE\uC9BF\uC9C0" + //   600 -   609
                "\uC9B1\uC9B2\uC9B3\uC9B4\uC9B5\uC9B6\uC9B7\uC9B8\uC9A9\uC9AA" + //   610 -   619
                "\uC9AB\uC9AC\uC9AD\uC9AE\uC9AF\uC9B0\uC9A1\uC9A2\uC9A3\uC9A4" + //   620 -   629
                "\uC9A5\uC9A6\uC9A7\uC9A8\uC8FB\uC8FC\u0000\uC8FD\u0000\uC8FE" + //   630 -   639
                "\u0000\u0000\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F" + //   640 -   649
                "\uC8FA\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF4B9\uF1B6" + //   650 -   659
                "\u0000\uF6FD\u0000\uDBF7\u0000\u0000\u0000\u0000\uFBEA\uC8F7" + //   660 -   669
                "\uC8F8\u0000\u0000\uC8F9\u0000\u0000\u0000\uEDA5\uEEF2\u0000" + //   670 -   679
                "\u0000\u0000\u0000\uDCF9\uC8F2\u0000\u0000\u0000\uC8F3\u0000" + //   680 -   689
                "\u0000\u0000\uE0E6\u0000\u0000\u0000\u0000\u0000\u0000\uD0BF" + //   690 -   699
                "\u0000\uFAAC\uC8EC\uC8ED\u0000\uC8EE\u0000\uC8EF\u0000\u0000" + //   700 -   709
                "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\uC8EA\uC8EB" + //   710 -   719
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF3F8\u0000\u0000\u0000" + //   720 -   729
                "\uFAFD\u0000\u0000\u0000\u0000\u0000\uD6AA\uC8E5\uC8E6\u0000" + //   730 -   739
                "\u0000\uC8E7\u0000\uC8E8\uC8E9\uC8E0\u0000\u0000\u0000\uC8E1" + //   740 -   749
                "\u0000\u0000\u0000\uDBED\u0000\u0000\u0000\u0000\u0000\u0000" + //   750 -   759
                "\uE6CB\u0000\uF5F1\uC8DA\uC8DB\u0000\uC8DC\u0000\uC8DD\u0000" + //   760 -   769
                "\u0000\u0008\u0009\n\u000B\u000C\r\u000E\u000F\uC8D9" + //   770 -   779
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE8E4\u0000\u0000" + //   780 -   789
                "\uA3A1\uA3A2\uA3A3\uA3A4\uA3A5\uA3A6\uA3A7\uC8D6\uC8D7\u0000" + //   790 -   799
                "\u0000\uC8D8\u0000\u0000\u0000\uD5D8\u0000\uF0BD\uD7D0\uD4D0" + //   800 -   809
                "\u0000\u0000\uC8F6\u0000\u0000\u0000\u0000\u0000\u0000\uDCEF" + //   810 -   819
                "\u0000\uD6A5\u0000\uE6FC\u0000\u0000\uD7FB\uD0D6\uDDF5\uF7F1" + //   820 -   829
                "\u0000\uA7BE\uA7E5\uA7E6\uA7E7\uA7E8\uA7E1\uA7E2\uA7E3\uC8D3" + //   830 -   839
                "\u0000\u0000\u0000\uC8D4\u0000\u0000\u0000\uCDEC\u0000\u0000" + //   840 -   849
                "\u0000\uDCB2\uD0EC\uCEFD\uC8CF\u0000\u0000\u0000\u0000\uC8D0" + //   850 -   859
                "\u0000\u0000\uC8F0\u0000\u0000\uC8F1\u0000\u0000\u0000\uD7D7" + //   860 -   869
                "\uDFA2\u0000\u0000\u0000\uCEBE\u0000\uCDE4\u0000\uD1AE\uDCED" + //   870 -   879
                "\uE8CE\u0000\uF0F9\uCEB5\uC8CE\u0000\u0000\u0000\u0000\u0000" + //   880 -   889
                "\u0000\u0000\uD4F1\u0000\u0000\uC8E4\u0000\u0000\u0000\u0000" + //   890 -   899
                "\u0000\u0000\uEBFA\u0000\uF9E6\u0000\uEFA2\uE2DA\uF6FC\u0000" + //   900 -   909
                "\u0000\uFBD0\uD1AD\u0000\uA7B2\uA7B3\uA7B4\uA7A7\uA7A8\uA7A9" + //   910 -   919
                "\uA7AA\uA7BD\uC8CC\u0000\u0000\u0000\uC8CD\u0000\u0000\u0000" + //   920 -   929
                "\uD6ED\u0000\u0000\u0000\u0000\u0000\u0000\uE3D5\uF5D0\u0000" + //   930 -   939
                "\uF1C1\u0000\u0000\uE2E9\uDCCA\uECB4\uFAC0\u0000\uA7A5\uA7AB" + //   940 -   949
                "\uA7AC\uA7AD\uA7AE\uA7AF\uA7B0\uA7B1\uC8C6\u0000\u0000\u0000" + //   950 -   959
                "\uC8C7\u0000\u0000\u0000\uCBFB\u0000\u0000\u0000\u0000\u0000" + //   960 -   969
                "\u0000\uF0C9\u0000\uFCFC\uC8C1\u0000\u0000\u0000\u0000\u0000" + //   970 -   979
                "\u0000\u0000\uD5CF\uD0F8\u0000\uDAD6\u0000\uCAB1\u0000\u0000" + //   980 -   989
                "\u0000\u0000\u0000\uB1F4\uB1F5\u0000\uB1F6\uC8BF\u0000\u0000" + //   990 -   999
                "\u0000\uC8C0\u0000\u0000\u0000\uCAA4\u0000\uDBF8\u0000\u0000" + //  1000 -  1009
                "\u0000\uDEC7\uC8BA\u0000\u0000\u0000\uC8BB\u0000\u0000\u0000" + //  1010 -  1019
                "\uF4D2\uE0BA\u0000\u0000\u0000\u0000\uDFC0\uC8B3\uC8B4\u0000" + //  1020 -  1029
                "\u0000\uC8B5\u0000\u0000\u0000\uCBF8\u0000\u0000\u0000\u0000" + //  1030 -  1039
                "\u0000\u0000\uF9F8\u0000\uDBCA\uC8AF\u0000\u0000\u0000\uC8B0" + //  1040 -  1049
                "\u0000\u0000\u0000\uFDA6\uEBEF\u0000\uF4A6\u0000\uCCCA\uF3A8" + //  1050 -  1059
                "\uC8A8\uC8A9\u0000\uC8AA\u0000\uC8AB\u0000\u0000\uC8D5\u0000" + //  1060 -  1069
                "\u0000\u0000\u0000\u0000\u0000\uDBCE\u0000\uF7C3\u0000\uD1AC" + //  1070 -  1079
                "\u0000\u0000\u0000\u0000\u0000\u0000\uDAC7\uC8A6\u0000\u0000" + //  1080 -  1089
                "\u0000\u0000\uC8A7\u0000\u0000\uC8CB\u0000\u0000\u0000\u0000" + //  1090 -  1099
                "\u0000\u0000\uECBD\uE2DC\uDEEB\uF0DC\uC8A3\uC8A4\u0000\u0000" + //  1100 -  1109
                "\uC8A5\u0000\u0000\u0000\uD9FA\uD3EE\u0000\u0000\u0000\uFAB8" + //  1110 -  1119
                "\u0000\uE2AE\u0000\uD3B7\uFACC\u0000\u0000\u0000\u0000\uC5C2" + //  1120 -  1129
                "\uC5C3\u0000\u0000\uE5DB\uF8F7\u0000\u0000\u0000\uF6D4\u0000" + //  1130 -  1139
                "\uA7D4\uA7D5\uA7D6\uA7D7\uA7D8\uA7A1\uA7A2\uA7A3\uC7FE\u0000" + //  1140 -  1149
                "\u0000\u0000\uC8A1\u0000\u0000\u0000\uF3DA\u0000\uCBC1\u0000" + //  1150 -  1159
                "\uDBC3\u0000\u0000\uC8C8\u0000\u0000\uC8C9\u0000\u0000\uC8CA" + //  1160 -  1169
                "\uC7F8\uC7F9\u0000\uC7FA\uC7FB\uC7FC\u0000\u0000\uC8C2\u0000" + //  1170 -  1179
                "\uC8C3\u0000\u0000\u0000\u0000\uE0AF\uF4E7\u0000\uEFDC\uCFFC" + //  1180 -  1189
                "\uC7F7\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF5C1\uDCC4" + //  1190 -  1199
                "\u0000\uE7BE\u0000\uFCF2\u0000\u0000\uD6B4\u0000\u0000\uFCC1" + //  1200 -  1209
                "\u0000\uEEAB\uD4A5\u0000\u0000\u0000\uF8CE\uF9F0\uE0ED\uE3B3" + //  1210 -  1219
                "\uF4B3\uC7F4\uC7F5\u0000\u0000\uC7F6\u0000\u0000\u0000\uCEFC" + //  1220 -  1229
                "\u0000\uDBC4\u0000\uF8F1\u0000\u0000\uC8BE\u0000\u0000\u0000" + //  1230 -  1239
                "\u0000\u0000\u0000\uCFD8\u0000\u0000\u0000\uCEE0\u0000\u0000" + //  1240 -  1249
                "\u0000\uDCD2\uFDEA\u0000\uD4BC\u0000\uFCEA\u0000\u0000\u0000" + //  1250 -  1259
                "\u0000\u0000\uB1E1\u0000\u0000\u0000\uEAC4\u0000\u0000\u0000" + //  1260 -  1269
                "\u0000\uEFD1\u0000\u0000\uD9B5\u0000\uA7BA\uA7BB\uA7DC\uA7DD" + //  1270 -  1279
                "\uA7DE\uA7B6\uA7B7\uA7B8\uC7EE\u0000\u0000\u0000\uC7EF\u0000" + //  1280 -  1289
                "\u0000\u0000\uE7EB\uF1D5\u0000\u0000\u0000\uF0BB\u0000\uEDEF" + //  1290 -  1299
                "\u0000\uE8A3\u0000\u0000\u0000\u0000\uCFF1\uC7E8\uC7E9\u0000" + //  1300 -  1309
                "\uC7EA\u0000\uC7EB\u0000\u0000\uC8AC\u0000\u0000\uC8AD\uC8AE" + //  1310 -  1319
                "\u0000\u0000\uC8B2\u0000\u0000\u0000\u0000\u0000\u0000\uCDC4" + //  1320 -  1329
                "\u0000\u0000\u0000\uF3DB\u0000\uDBA7\uF6B7\u0000\uCFE6\uF0F2" + //  1330 -  1339
                "\uC7E6\u0000\uC7E7\u0000\u0000\u0000\u0000\u0000\uBDBF\uBDC0" + //  1340 -  1349
                "\u0000\uBDC1\uC7E3\uC7E4\u0000\u0000\uC7E5\u0000\u0000\u0000" + //  1350 -  1359
                "\uEAE2\u0000\u0000\u0000\u0000\uD7C2\u0000\uD7E1\uFAF5\u0000" + //  1360 -  1369
                "\u0000\uD5C9\uF8AC\u0000\u0000\uFCD1\u0000\uEDB2\uF4AF\u0000" + //  1370 -  1379
                "\uFBA3\u0000\uA7C9\uA7CA\uA7CB\uA7CC\uA7CD\u0000\u0000\u0000" + //  1380 -  1389
                "\uFBC6\uCFB3\u0000\u0000\u0000\uD1A8\u0000\u0000\u0000\u0000" + //  1390 -  1399
                "\uC4BF\uC4C0\u0000\u0000\uF3D2\u0000\u0000\uEEF4\u0000\uE2D3" + //  1400 -  1409
                "\u0000\u0098\u0099\u009A\u009B\u009C\u009D\u009E\u009F\uC7E1" + //  1410 -  1419
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD3EB\u0000\uE9D4" + //  1420 -  1429
                "\uC7DF\uC7E0\u0000\u0000\u0000\u0000\u0000\u0000\uE1D9\u0000" + //  1430 -  1439
                "\u0000\uEFAB\uC7DA\u0000\u0000\u0000\uC7DB\u0000\u0000\u0000" + //  1440 -  1449
                "\uE3E0\u0000\uCAC9\uF2E9\u0000\uD5CE\u0000\uFBCF\u0000\u0000" + //  1450 -  1459
                "\u0000\u0000\u0000\u0000\uCFB7\uC7D4\uC7D5\u0000\uC7D6\u0000" + //  1460 -  1469
                "\uC7D7\u0000\u0000\uC7F3\u0000\u0000\u0000\u0000\u0000\u0000" + //  1470 -  1479
                "\uE5A2\u0000\u0000\u0000\uE9B5\u0000\uCCC9\uFAD5\u0000\u0000" + //  1480 -  1489
                "\uE1D4\uC7D2\u0000\u0000\u0000\u0000\uC7D3\u0000\u0000\uC7CE" + //  1490 -  1499
                "\u0000\u0000\u0000\u0000\u0000\u0000\uD6A3\u0000\u0000\u0000" + //  1500 -  1509
                "\uE5F9\uECEA\uDDD6\uEDC2\u0000\u0000\u0000\uF9C1\u0000\u0000" + //  1510 -  1519
                "\u0000\u0000\uC4D1\u0000\u0000\u0000\uFBF2\u0000\uDBF6\u0000" + //  1520 -  1529
                "\uDEDF\uC7CF\uC7D0\u0000\u0000\uC7D1\u0000\u0000\u0000\uF8A5" + //  1530 -  1539
                "\u0000\u0000\u0000\u0000\u0000\uE5BA\uC7C9\u0000\u0000\u0000" + //  1540 -  1549
                "\uC7CA\u0000\u0000\u0000\uF9F2\uECA5\uD0DF\u0000\uE7EA\uD0EB" + //  1550 -  1559
                "\uDCD1\uC7C2\u0000\u0000\u0000\uC7C3\u0000\u0000\u0000\uD0EA" + //  1560 -  1569
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCCA9\uFDC6\u0000\uDFD2" + //  1570 -  1579
                "\u0000\uCECA\u0000\uEEDA\u0000\u0000\u0000\uD8C4\u0000\u0000" + //  1580 -  1589
                "\u0000\u0000\uD0F5\u0000\u0000\uE8ED\uD0D2\uC7BE\u0000\u0000" + //  1590 -  1599
                "\uC7BF\u0000\uC7C0\u0000\u0000\uC7B5\u0000\u0000\u0000\u0000" + //  1600 -  1609
                "\u0000\u0000\uE2B8\uEBAB\u0000\u0000\uC6F5\u0000\uC6F6\u0000" + //  1610 -  1619
                "\u0000\u0000\u0000\uE0C1\uEFDB\u0000\u0000\uF0E9\uC7BD\u0000" + //  1620 -  1629
                "\u0000\u0000\u0000\u0000\u0000\u0000\uFBFB\u0000\u0000\uC6FE" + //  1630 -  1639
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF0FC\u0000\u0000\u0000" + //  1640 -  1649
                "\uAAA1\uAAA2\uAAA3\uAAA4\uAAA5\uAAA6\uAAA7\uC7BB\u0000\u0000" + //  1650 -  1659
                "\u0000\uC7BC\u0000\u0000\u0000\uD8CC\uF9F1\u0000\uCEDF\uFAA4" + //  1660 -  1669
                "\uE6B2\u0000\uDFF9\uD7E0\u0000\u0000\u0000\u0000\u0000\u0000" + //  1670 -  1679
                "\uE9E4\u0000\u0000\uE4A2\u0000\uE2E8\u0000\uE6D0\u0000\uFBE8" + //  1680 -  1689
                "\uC7B7\u0000\u0000\u0000\uC7B8\u0000\u0000\u0000\uF5E4\u0000" + //  1690 -  1699
                "\u0000\uF3A6\uDDE0\uE1A6\u0000\uD4BA\uE4B3\u0000\uE9DA\u0000" + //  1700 -  1709
                "\uDEB6\u0000\uD9BF\uC7B0\uC7B1\u0000\uC7B2\u0000\uC7B3\u0000" + //  1710 -  1719
                "\u0000\uC6AD\u0000\u0000\u0000\u0000\u0000\u0000\uDAD8\uD1B9" + //  1720 -  1729
                "\u0000\uDFA9\uC7AE\u0000\uC7AF\u0000\u0000\u0000\u0000\u0000" + //  1730 -  1739
                "\uBDBA\uBDBB\u0000\u0000\uC5FC\u0000\u0000\u0000\u0000\u0000" + //  1740 -  1749
                "\u0000\uE0FE\u0000\uDFFB\u0000\uF7FA\u0000\u0000\u0000\uF8AB" + //  1750 -  1759
                "\u0000\u0000\u0000\uCEF5\u0000\u0000\u0000\u0000\uDDC5\u0000" + //  1760 -  1769
                "\u0000\u0000\u0000\uC1AA\uC1AB\u0000\uC1AC\uC7AA\uC7AB\u0000" + //  1770 -  1779
                "\u0000\uC7AC\u0000\u0000\uC7AD\uC7A6\u0000\u0000\u0000\uC7A7" + //  1780 -  1789
                "\u0000\u0000\u0000\uA4A1\uA4A2\uA4A3\uA4A4\uA4A5\uA4A6\uA4A7" + //  1790 -  1799
                "\uC7A3\u0000\u0000\u0000\uC7A4\u0000\u0000\u0000\uABA1\uABA2" + //  1800 -  1809
                "\uABA3\uABA4\uABA5\uABA6\uABA7\uC7A1\u0000\u0000\u0000\u0000" + //  1810 -  1819
                "\u0000\u0000\u0000\uF6E6\u0000\u0000\uC5F3\u0000\u0000\u0000" + //  1820 -  1829
                "\u0000\u0000\u0000\uF0AF\uD6BD\u0000\u0000\uC3C3\u0000\u0000" + //  1830 -  1839
                "\u0000\u0000\u0000\u0000\uCEB4\u0000\u0000\u0000\uE8AD\u0000" + //  1840 -  1849
                "\u0000\u0000\u0000\uEFAF\uC6F9\u0000\u0000\u0000\uC6FA\u0000" + //  1850 -  1859
                "\u0000\u0000\uA2B2\u0000\u0000\u0000\u0000\u0000\u0000\uE6ED" + //  1860 -  1869
                "\u0000\u0000\uC5DE\u0000\u0000\u0000\u0000\u0000\u0000\uF3C1" + //  1870 -  1879
                "\uD0AB\u0000\uD4E4\uC6F4\u0000\u0000\u0000\u0000\u0000\u0000" + //  1880 -  1889
                "\u0000\uF2C8\u0000\u0000\uC5B7\u0000\u0000\u0000\u0000\u0000" + //  1890 -  1899
                "\u0000\uD6DE\uE4F4\uE1EF\u0000\uDFD1\u0000\u0000\u0000\u0000" + //  1900 -  1909
                "\u0000\uEDED\uF8B8\uC6F3\u0000\u0000\u0000\u0000\u0000\u0000" + //  1910 -  1919
                "\u0000\uE6BF\u0000\u0000\uC5AF\u0000\u0000\u0000\u0000\u0000" + //  1920 -  1929
                "\u0000\uDDDD\u0000\u0000\u0000\uF3BB\u0000\uE5E1\u0000\u0000" + //  1930 -  1939
                "\u0000\uFBED\u0000\uE0AD\u0000\u0000\uEAEE\uC6F1\uC6F2\u0000" + //  1940 -  1949
                "\u0000\u0000\u0000\u0000\u0000\uD7E6\u0000\u0000\u0000\uA2E5" + //  1950 -  1959
                "\uA2E2\u0000\u0000\u0000\uA7D9\u0000\uEABA\u0000\uEAD3\u0000" + //  1960 -  1969
                "\u0000\uEDC9\uDDAB\u0000\uA8C9\uA8CA\uA8CB\uA8CC\u0000\u0000" + //  1970 -  1979
                "\u0000\uA2DE\uC6ED\u0000\u0000\u0000\uC6EE\u0000\u0000\u0000" + //  1980 -  1989
                "\uA2B5\u0000\u0000\u0000\u0000\u0000\u0000\uF3A3\u0000\uD3EC" + //  1990 -  1999
                "\uC6E8\uC6E9\u0000\uC6EA\u0000\uC6EB\u0000\u0000\uC5A4\u0000" + //  2000 -  2009
                "\u0000\u0000\u0000\u0000\u0000\uD7FE\u0000\u0000\u0000\uD1CB" + //  2010 -  2019
                "\uD6E4\u0000\u0000\u0000\uD5F2\uC6E7\u0000\u0000\u0000\u0000" + //  2020 -  2029
                "\u0000\u0000\u0000\uE1F5\uF1B3\u0000\uD6D3\u0000\u0000\u0000" + //  2030 -  2039
                "\u0000\u0000\u0000\u0000\uDBFC\uC6E4\uC6E5\u0000\u0000\uC6E6" + //  2040 -  2049
                "\u0000\u0000\u0000\uA9FB\uA9FC\uA9FD\uA9FE\u0000\u0000\u0000" + //  2050 -  2059
                "\uF2CE\uDBB4\u0000\u0000\u0000\uF7B4\u0000\u0000\u0000\u0000" + //  2060 -  2069
                "\uC3BC\uC3BD\u0000\u0000\uC4F8\u0000\u0000\u0000\u0000\u0000" + //  2070 -  2079
                "\u0000\uE2EB\uD6FC\u0000\u0000\uBFEB\u0000\u0000\u0000\u0000" + //  2080 -  2089
                "\u0000\u0000\uDEB0\u0000\u0000\u0000\uF2E0\uF1C9\u0000\u0000" + //  2090 -  2099
                "\u0000\u0000\uF4EF\u0000\u0000\u0000\uF6CE\uC6E2\uC6E3\u0000" + //  2100 -  2109
                "\u0000\u0000\u0000\u0000\u0000\uF1C3\uEEDF\u0000\u0000\uC4E9" + //  2110 -  2119
                "\u0000\u0000\u0000\u0000\u0000\u0000\uEEDC\u0000\uCBCB\uFCD5" + //  2120 -  2129
                "\uC6DD\u0000\u0000\u0000\uC6DE\u0000\u0000\u0000\uACD7\u0000" + //  2130 -  2139
                "\u0000\u0000\u0000\u0000\u0000\uE4DD\uDFEE\uCBAC\uC6D4\uC6D5" + //  2140 -  2149
                "\u0000\uC6D6\uC6D7\uC6D8\u0000\u0000\uC4E1\u0000\u0000\u0000" + //  2150 -  2159
                "\u0000\u0000\u0000\uE4DF\u0000\uCAD6\u0000\uFAD2\u0000\u0000" + //  2160 -  2169
                "\u0000\u0000\u0000\uF8EF\u0000\uA8C1\uA8C2\uA8C3\uA8C4\uA8C5" + //  2170 -  2179
                "\uA8C6\uA8C7\uA8C8\uC6D3\u0000\u0000\u0000\u0000\u0000\u0000" + //  2180 -  2189
                "\u0000\uFDAB\u0000\uDFE0\uC6D0\uC6D1\u0000\u0000\uC6D2\u0000" + //  2190 -  2199
                "\u0000\u0000\uACA7\u0000\u0000\u0000\u0000\u0000\u0000\uEEBC" + //  2200 -  2209
                "\u0000\uEFC1\uC6CD\uC6CE\u0000\u0000\u0000\uC6CF\u0000\u0000" + //  2210 -  2219
                "\uC4BE\u0000\u0000\u0000\u0000\u0000\u0000\uF4BD\u0000\uCFB8" + //  2220 -  2229
                "\uE9DB\uC6C7\u0000\u0000\u0000\uC6C8\u0000\uC6C9\u0000\uD7F2" + //  2230 -  2239
                "\u0000\uE1C0\u0000\uDBE2\uE6D8\u0000\u0000\uE2D7\u0000\u0000" + //  2240 -  2249
                "\u0000\u0000\u0000\u0000\uE1A3\uD2E0\u0000\uA8B9\uA8BA\uA8BB" + //  2250 -  2259
                "\uA8BC\uA8BD\uA8BE\uA8BF\uA8C0\uC6C0\uC6C1\u0000\uC6C2\u0000" + //  2260 -  2269
                "\uC6C3\u0000\u0000\uC4B2\u0000\u0000\u0000\u0000\u0000\u0000" + //  2270 -  2279
                "\uFADC\u0000\uEDB5\uE1E3\uC6BF\u0000\u0000\u0000\u0000\u0000" + //  2280 -  2289
                "\u0000\u0000\uE6F4\u0000\u0000\uC3F6\u0000\u0000\u0000\u0000" + //  2290 -  2299
                "\u0000\u0000\uCCB3\u0000\u0000\uDBF3\uC6BC\uC6BD\u0000\u0000" + //  2300 -  2309
                "\uC6BE\u0000\u0000\u0000\uA5E1\uA5E2\uA5E3\uA5E4\uA5E5\uA5E6" + //  2310 -  2319
                "\uA5E7\uC6B8\u0000\u0000\u0000\uC6B9\u0000\u0000\u0000\uA5C1" + //  2320 -  2329
                "\uA5C2\uA5C3\uA5C4\uA5C5\uA5C6\uA5C7\uC6B4\uC6B5\u0000\uC6B6" + //  2330 -  2339
                "\u0000\u0000\u0000\u0000\uEDC5\uF3D6\u0000\u0000\uDED9\uC6B2" + //  2340 -  2349
                "\u0000\uC6B3\u0000\u0000\u0000\u0000\u0000\uBDB0\uBDB1\u0000" + //  2350 -  2359
                "\uBDB2\uC6AE\uC6AF\u0000\u0000\uC6B0\u0000\u0000\uC6B1\uC6AA" + //  2360 -  2369
                "\u0000\u0000\u0000\uC6AB\u0000\u0000\u0000\uA9B0\uA8AF\uA9AF" + //  2370 -  2379
                "\u0000\u0000\u0000\u0000\uBDDC\uBDDD\u0000\u0000\uC3E6\u0000" + //  2380 -  2389
                "\u0000\u0000\u0000\u0000\u0000\uE8A2\u0000\u0000\u0000\uEAC2" + //  2390 -  2399
                "\uF2E6\uF0B6\u0000\u0000\u0000\uD8B5\u0000\u0000\u0000\uE4DC" + //  2400 -  2409
                "\u0000\uD4F3\uD4C9\u0000\u0000\u0000\u0000\uD6FA\u0000\uA8B1" + //  2410 -  2419
                "\uA8B2\uA8B3\uA8B4\uA8B5\uA8B6\uA8B7\uA8B8\uC6A6\uC6A7\u0000" + //  2420 -  2429
                "\u0000\u0000\uC6A8\u0000\u0000\uC3DC\u0000\u0000\u0000\u0000" + //  2430 -  2439
                "\u0000\u0000\uE3D8\u0000\u0000\u0000\uF8A1\u0000\u0000\u0000" + //  2440 -  2449
                "\uE8D6\u0000\uE7BC\u0000\u0000\u0000\u0000\u0000\uD1EE\u0000" + //  2450 -  2459
                "\uA9C9\uA9CA\uA9CB\uA9CC\uA2DF\u0000\u0000\u0000\uD9EC\u0000" + //  2460 -  2469
                "\uD9BD\u0000\uD8DF\uC6A5\u0000\u0000\u0000\u0000\u0000\u0000" + //  2470 -  2479
                "\u0000\uF1ED\u0000\uCCCF\uC6A2\uC6A3\u0000\u0000\uC6A4\u0000" + //  2480 -  2489
                "\u0000\u0000\uA9A5\uA8A6\uA9A6\u0000\u0000\u0000\u0000\uBDC3" + //  2490 -  2499
                "\uBDC4\u0000\u0000\uC3D5\u0000\u0000\u0000\u0000\u0000\u0000" + //  2500 -  2509
                "\uD8E0\uFCBA\uFDAF\uF0CE\uC5FD\u0000\u0000\u0000\u0000\u0000" + //  2510 -  2519
                "\u0000\u0000\uF8D0\u0000\u0000\uC3C9\u0000\u0000\u0000\u0000" + //  2520 -  2529
                "\u0000\u0000\uFDB6\u0000\u0000\u0000\uDCF2\u0000\u0000\u0000" + //  2530 -  2539
                "\u0000\u0000\uB8B2\uB8B3\u0000\uB8B4\uC5F7\u0000\u0000\u0000" + //  2540 -  2549
                "\uC5F8\u0000\u0000\u0000\uA9A2\u0000\u0000\u0000\u0000\u0000" + //  2550 -  2559
                "\u0000\uE1DE\uCBEE\u0000\uE6FB\u0000\u0000\u0000\u0000\u0000" + //  2560 -  2569
                "\uE6D4\u0000\uA9C1\uA9C2\uA9C3\uA9C4\uA9C5\uA9C6\uA9C7\uA9C8" + //  2570 -  2579
                "\uC5F4\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF8F2\u0000" + //  2580 -  2589
                "\uF4F9\uC5F1\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF4F8" + //  2590 -  2599
                "\u0000\u0000\uC2F4\uC2F5\u0000\u0000\u0000\u0000\uC2F6\uC5EF" + //  2600 -  2609
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD6A9\u0000\u0000" + //  2610 -  2619
                "\uC2ED\u0000\u0000\u0000\u0000\u0000\u0000\uD7B6\uCFB5\u0000" + //  2620 -  2629
                "\uD9A8\uC5EE\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF3D0" + //  2630 -  2639
                "\u0000\u0000\uC2DD\u0000\u0000\u0000\u0000\u0000\u0000\uE2A9" + //  2640 -  2649
                "\u0000\u0000\uDEBC\uC5E8\uC5E9\u0000\uC5EA\u0000\uC5EB\u0000" + //  2650 -  2659
                "\u0000\uC2D0\u0000\uC2D1\u0000\u0000\u0000\u0000\uD5FD\u0000" + //  2660 -  2669
                "\u0000\u0000\u0000\uC2EB\u0000\u0000\uC2EC\uC5E7\u0000\u0000" + //  2670 -  2679
                "\u0000\u0000\u0000\u0000\u0000\uE8E9\u0000\uE3AC\uC5E4\uC5E5" + //  2680 -  2689
                "\u0000\u0000\uC5E6\u0000\u0000\u0000\uA2AE\u0000\u0000\uA2B4" + //  2690 -  2699
                "\u0000\u0000\uA1D7\uC5E3\u0000\u0000\u0000\u0000\u0000\u0000" + //  2700 -  2709
                "\u0000\uD0C1\u0000\u0000\uC2BB\u0000\u0000\u0000\u0000\u0000" + //  2710 -  2719
                "\u0000\uCAF4\u0000\u0000\u0000\uEEE8\uDADE\u0000\uF2F7\u0000" + //  2720 -  2729
                "\u0000\uB9D8\u0000\u0000\uB9D9\uB9DA\uB9DB\uB9DC\uC5DF\u0000" + //  2730 -  2739
                "\u0000\u0000\uC5E0\u0000\u0000\u0000\uC5EC\u0000\uC5ED\u0000" + //  2740 -  2749
                "\u0000\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\uC5D9" + //  2750 -  2759
                "\u0000\u0000\u0000\uC5DA\u0000\u0000\u0000\uC0BC\u0000\uC0BD" + //  2760 -  2769
                "\uC0BE\u0000\uC0BF\uC5D2\uC5D3\u0000\uC5D4\uC5D5\uC5D6\u0000" + //  2770 -  2779
                "\u0000\uC2A3\uC2A4\u0000\uC2A5\uC2A6\u0000\u0000\uB9A5\u0000" + //  2780 -  2789
                "\u0000\u0000\u0000\u0000\u0000\uF3DE\u0000\u0000\u0000\uDEE3" + //  2790 -  2799
                "\u0000\u0000\u0000\u0000\u0000\uB2C4\uB2C5\u0000\uB2C6\uC5D0" + //  2800 -  2809
                "\u0000\uC5D1\u0000\u0000\u0000\u0000\u0000\uBDAC\uBDAD\u0000" + //  2810 -  2819
                "\u0000\uC1DF\u0000\u0000\u0000\u0000\u0000\u0000\uD1A5\uDCB8" + //  2820 -  2829
                "\u0000\u0000\uB7F8\u0000\u0000\u0000\u0000\u0000\u0000\uF6F3" + //  2830 -  2839
                "\u0000\u0000\u0000\uF4E1\uF9B5\u0000\u0000\uE1C3\uE1C2\uC5CD" + //  2840 -  2849
                "\uC5CE\u0000\u0000\uC5CF\u0000\u0000\u0000\uB4D8\u0000\uB4D9" + //  2850 -  2859
                "\uB4DA\uB4DB\u0000\uFBF4\uD5A7\u0000\u0000\u0000\uF1F6\u0000" + //  2860 -  2869
                "\uE6D3\uC5CB\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uDBA8" + //  2870 -  2879
                "\u0000\u0000\uC1D1\u0000\u0000\u0000\u0000\u0000\u0000\uDCB7" + //  2880 -  2889
                "\u0000\u0000\u0000\uEBB3\u0000\uF0B4\u0000\u0000\uCBF4\uC5C9" + //  2890 -  2899
                "\uC5CA\u0000\u0000\u0000\u0000\u0000\u0000\uECF9\u0000\u0000" + //  2900 -  2909
                "\uF8AD\uC5C4\u0000\u0000\u0000\uC5C5\u0000\u0000\u0000\uB4CB" + //  2910 -  2919
                "\u0000\uB4CC\u0000\u0000\u0000\uCFED\u0000\uEDEB\u0000\u0000" + //  2920 -  2929
                "\u0000\uF0E5\u0000\u0000\u0000\uF4C4\uC5BD\uC5BE\u0000\uC5BF" + //  2930 -  2939
                "\uC5C0\uC5C1\u0000\u0000\uC1C7\u0000\u0000\u0000\u0000\u0000" + //  2940 -  2949
                "\u0000\uD4E1\u0000\uD1A3\uE1D6\uC5BB\uC5BC\u0000\u0000\u0000" + //  2950 -  2959
                "\u0000\u0000\u0000\uF6DF\u0000\uF2DA\uE4EB\uC5B8\uC5B9\u0000" + //  2960 -  2969
                "\u0000\uC5BA\u0000\u0000\u0000\uB3F4\uB3F5\uB3F6\u0000\u0000" + //  2970 -  2979
                "\u0000\uDFF8\u0000\u0000\u0000\u0000\u0000\uECC7\u0000\u0000" + //  2980 -  2989
                "\u0000\uEDFC\u0000\u0000\uE1DB\u0000\uD6AD\u0000\u0000\uFDCE" + //  2990 -  2999
                "\u0000\u0000\u0000\uE8A1\uC5B2\u0000\u0000\u0000\uC5B3\u0000" + //  3000 -  3009
                "\u0000\u0000\uB1ED\u0000\uB1EE\uB1EF\uB1F0\u0000\uEDC8\uEFC3" + //  3010 -  3019
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCDC9\uF9B7\u0000\uA9B9" + //  3020 -  3029
                "\uA9BA\uA9BB\uA9BC\uA9BD\uA9BE\uA9BF\uA9C0\uC5AB\u0000\u0000" + //  3030 -  3039
                "\u0000\uC5AC\u0000\u0000\u0000\uF6CD\u0000\u0000\u0000\u0000" + //  3040 -  3049
                "\u0000\uBADF\uBAE0\u0000\u0000\uC1AD\u0000\u0000\u0000\u0000" + //  3050 -  3059
                "\u0000\u0000\uDEFD\uF2F9\u0000\uD5C7\uC5A8\u0000\u0000\u0000" + //  3060 -  3069
                "\u0000\u0000\u0000\u0000\uCBED\u0000\u0000\uC0C0\uC0C1\uC0C2" + //  3070 -  3079
                "\uC0C3\uC0C4\uC0C5\uC0C6\uC5A7\u0000\u0000\u0000\u0000\u0000" + //  3080 -  3089
                "\u0000\u0000\uFAA5\u0000\u0000\uC0AE\u0000\u0000\u0000\u0000" + //  3090 -  3099
                "\u0000\u0000\uFDBE\u0000\u0000\uCAAC\uC5A5\u0000\u0000\u0000" + //  3100 -  3109
                "\uC5A6\u0000\u0000\u0000\uF0BA\uEEB1\u0000\u0000\uEEB2\u0000" + //  3110 -  3119
                "\uFDDC\uEDB3\uCEC9\u0000\u0000\u0000\u0000\u0000\uB1AF\uB1B0" + //  3120 -  3129
                "\u0000\uB1B1\uC4FD\u0000\u0000\u0000\uC4FE\u0000\u0000\u0000" + //  3130 -  3139
                "\uE5A4\u0000\u0000\u0000\uD5B6\u0000\uCFB6\u0000\u0000\u0000" + //  3140 -  3149
                "\uEDC7\uEEAC\u0000\u0000\uE0DD\uFBF3\u0000\u0000\u0000\u0000" + //  3150 -  3159
                "\u0000\uB0B7\uB0B8\u0000\uB0B9\uC4F9\u0000\u0000\u0000\u0000" + //  3160 -  3169
                "\u0000\u0000\u0000\uF6D5\uD5E2\u0000\uFCE9\u0000\u0000\u0000" + //  3170 -  3179
                "\u0000\u0000\u0000\u0000\uD9A7\uC4F6\u0000\u0000\u0000\uC4F7" + //  3180 -  3189
                "\u0000\u0000\u0000\uF6AF\u0000\u0000\u0000\u0000\u0000\uBACC" + //  3190 -  3199
                "\u0000\u0000\u0000\uD2C1\u0000\u0000\u0000\uF8D7\u0000\uF7A8" + //  3200 -  3209
                "\u0000\u0000\u0000\u0000\uFBCE\u0000\u0000\uE2AA\u0000\uD5A6" + //  3210 -  3219
                "\u0000\u0000\uD4D7\u0000\uA9B1\uA9B2\uA9B3\uA9B4\uA9B5\uA9B6" + //  3220 -  3229
                "\uA9B7\uA9B8\uC4F1\uC4F2\u0000\uC4F3\u0000\uC4F4\u0000\u0000" + //  3230 -  3239
                "\uBFDB\u0000\u0000\u0000\u0000\u0000\u0000\uCCD7\uE5C2\u0000" + //  3240 -  3249
                "\u0000\uB3B9\u0000\uB3BA\uB3BB\uB3BC\u0000\u0000\uD4AD\uF6D1" + //  3250 -  3259
                "\u0000\u0000\u0000\u0000\u0000\uB6C6\u0000\u0000\u0000\uE7F0" + //  3260 -  3269
                "\u0000\uD0EE\u0000\u0000\uF3AA\uC4F0\u0000\u0000\u0000\u0000" + //  3270 -  3279
                "\u0000\u0000\u0000\uF1E9\u0000\u0000\uBFCB\u0000\uBFCC\u0000" + //  3280 -  3289
                "\u0000\u0000\u0000\uE1B7\u0000\u0000\u0000\u0000\uC2D5\u0000" + //  3290 -  3299
                "\u0000\u0000\uD0CC\u0000\u0000\u0000\u0000\u0000\uB6CC\u0000" + //  3300 -  3309
                "\u0000\u0000\uF0BF\u0000\uF6A4\u0000\uE3B6\u0000\uFBA5\uE1EE" + //  3310 -  3319
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCBCE\uFBD8\u0000\uA4F8" + //  3320 -  3329
                "\uA4F9\uA4FA\uA4FB\uA4FC\uA4FD\uA4FE\u0000\u0090\u0091\u0092" + //  3330 -  3339
                "\u0093\u0094\u0095\u0096\u0097\uC4ED\uC4EE\u0000\u0000\uC4EF" + //  3340 -  3349
                "\u0000\u0000\u0000\uEDCE\u0000\u0000\u0000\u0000\u0000\uBAC4" + //  3350 -  3359
                "\u0000\u0000\u0000\uFCE0\uD7C8\uFDAD\u0000\u0000\u0000\uD4E5" + //  3360 -  3369
                "\u0000\u0000\u0000\uF9C3\uC4EB\u0000\u0000\u0000\u0000\u0000" + //  3370 -  3379
                "\u0000\u0000\uA1DA\uA1D9\u0000\uD6EE\u0000\u0000\u0000\u0000" + //  3380 -  3389
                "\u0000\u0000\uE7BB\uC4EA\u0000\u0000\u0000\u0000\u0000\u0000" + //  3390 -  3399
                "\u0000\uA1D1\u0000\u0000\uBEED\u0000\u0000\u0000\u0000\u0000" + //  3400 -  3409
                "\u0000\uEBF4\u0000\u0000\u0000\uD3B2\u0000\u0000\u0000\uE2C0" + //  3410 -  3419
                "\uF2DF\uC4E6\u0000\u0000\u0000\u0000\uC4E7\u0000\u0000\uBEE7" + //  3420 -  3429
                "\u0000\u0000\u0000\uBEE8\u0000\uBEE9\uC4E5\u0000\u0000\u0000" + //  3430 -  3439
                "\u0000\u0000\u0000\uA1AD\uA1EF\u0000\u0000\uBEB1\u0000\u0000" + //  3440 -  3449
                "\u0000\u0000\u0000\u0000\uDAE6\uF7B3\u0000\u0000\uB1A8\u0000" + //  3450 -  3459
                "\u0000\uB1A9\uB1AA\u0000\u0000\uFBF6\u0000\u0000\u0000\u0000" + //  3460 -  3469
                "\u0000\u0000\uDEFA\u0000\uD7F8\uC4E2\uC4E3\u0000\u0000\uC4E4" + //  3470 -  3479
                "\u0000\u0000\u0000\uE8A7\u0000\u0000\u0000\u0000\u0000\uBAC3" + //  3480 -  3489
                "\u0000\u0000\u0000\uFBA1\u0000\u0000\u0000\uE5E9\uE9EE\uC4DC" + //  3490 -  3499
                "\u0000\u0000\u0000\uC4DD\u0000\u0000\u0000\uE5E0\u0000\u0000" + //  3500 -  3509
                "\u0000\u0000\u0000\uBABD\uBABE\u0000\uBABF\uC4D9\u0000\u0000" + //  3510 -  3519
                "\u0000\u0000\u0000\u0000\u0000\uE4EC\u0000\uD6D2\u0000\uF9D5" + //  3520 -  3529
                "\uE7BA\uEBD5\uD5F7\uEFE7\uE1BE\uC4D7\uC4D8\u0000\u0000\u0000" + //  3530 -  3539
                "\u0000\u0000\u0000\uCDA3\u0000\u0000\uE8E6\uC4D2\u0000\u0000" + //  3540 -  3549
                "\u0000\uC4D3\u0000\u0000\u0000\uF7E3\u0000\u0000\u0000\u0000" + //  3550 -  3559
                "\u0000\uBAB8\uBAB9\uBABA\u0000\uFBCC\uEBA1\u0000\u0000\uD4A6" + //  3560 -  3569
                "\u0000\u0000\u0000\uEBE5\u0000\u0000\uE1D2\u0000\uA4F0\uA4F1" + //  3570 -  3579
                "\uA4F2\uA4F3\uA4F4\uA4F5\uA4F6\uA4F7\uC4CD\uC4CE\u0000\uC4CF" + //  3580 -  3589
                "\u0000\uC4D0\u0000\u0000\uBEAB\u0000\u0000\u0000\u0000\u0000" + //  3590 -  3599
                "\u0000\uE2A4\u0000\u0000\u0000\uDAA5\u0000\uDBBD\u0000\u0000" + //  3600 -  3609
                "\u0000\uF9DC\u0000\u0000\uF3DC\u0000\u0000\uB1A4\u0000\u0000" + //  3610 -  3619
                "\u0000\u0000\u0000\u0000\uEDA8\uDEC2\uF6E2\uEDDC\uC4CC\u0000" + //  3620 -  3629
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF4ED\u0000\uCCAD\uF6FA" + //  3630 -  3639
                "\uD6B2\uD2D8\u0000\u0000\u0000\u0000\uC4FB\uC4FC\u0000\u0000" + //  3640 -  3649
                "\uF8DC\uF7EE\uEBE8\u0000\uD2FA\u0000\u0000\uFCB6\uF2AD\uEFE1" + //  3650 -  3659
                "\uF3AE\uDCC6\uD9EB\u0000\uA4E8\uA4E9\uA4EA\uA4EB\uA4EC\uA4ED" + //  3660 -  3669
                "\uA4EE\uA4EF\uC4C9\uC4CA\u0000\u0000\uC4CB\u0000\u0000\u0000" + //  3670 -  3679
                "\uE2D1\u0000\u0000\u0000\u0000\u0000\uBAAD\uBAAE\u0000\u0000" + //  3680 -  3689
                "\uBDCC\u0000\u0000\u0000\u0000\uBDCD\u0000\uE9FA\u0000\u0000" + //  3690 -  3699
                "\u0000\uFBCB\u0000\u0000\uCAD5\uC4C7\uC4C8\u0000\u0000\u0000" + //  3700 -  3709
                "\u0000\u0000\u0000\uE7C2\u0000\u0000\u0000\uD5F0\u0000\u0000" + //  3710 -  3719
                "\uD1CA\u0000\u0000\uBDC2\u0000\u0000\u0000\u0000\u0000\u0000" + //  3720 -  3729
                "\uEAB5\u0000\uE5AA\uDFBA\uC4C1\u0000\u0000\uC4C2\uC4C3\u0000" + //  3730 -  3739
                "\u0000\u0000\uCCF3\uE6BE\u0000\u0000\u0000\uF6AE\uC4B7\uC4B8" + //  3740 -  3749
                "\u0000\uC4B9\uC4BA\uC4BB\u0000\u0000\uBDB3\u0000\u0000\u0000" + //  3750 -  3759
                "\u0000\u0000\u0000\uDDC9\u0000\u0000\uD4D3\uC4B6\u0000\u0000" + //  3760 -  3769
                "\u0000\u0000\u0000\u0000\u0000\uEFC6\u0000\uF4CD\u0000\u0000" + //  3770 -  3779
                "\u0000\u0000\uF1BF\uF8B1\u0000\uA4E0\uA4E1\uA4E2\uA4E3\uA4E4" + //  3780 -  3789
                "\uA4E5\uA4E6\uA4E7\uC4B3\uC4B4\u0000\u0000\uC4B5\u0000\u0000" + //  3790 -  3799
                "\u0000\uD8CB\u0000\u0000\u0000\u0000\u0000\uB9E3\uB9E4\u0000" + //  3800 -  3809
                "\uB9E5\uC4AD\u0000\u0000\u0000\uC4AE\u0000\u0000\u0000\uD5FA" + //  3810 -  3819
                "\u0000\uE4F7\u0000\u0000\u0000\uDFCB\u0000\u0000\u0000\u0000" + //  3820 -  3829
                "\u0000\uFBE0\uF2E5\u0000\u0000\uBDA2\uBDA3\u0000\uBDA4\u0000" + //  3830 -  3839
                "\u0000\u0000\uFAF8\u0000\u0000\u0000\u0000\u0000\uB4BC\u0000" + //  3840 -  3849
                "\u0000\u0000\uEAE3\u0000\u0000\u0000\u0000\u0000\uE0D1\u0000" + //  3850 -  3859
                "\u0000\uE9A8\uC4A7\uC4A8\u0000\uC4A9\u0000\uC4AA\u0000\u0000" + //  3860 -  3869
                "\uBCF5\u0000\u0000\u0000\u0000\u0000\u0000\uE3E5\u0000\uCBC5" + //  3870 -  3879
                "\uEAB4\uC4A5\uC4A6\u0000\u0000\u0000\u0000\u0000\u0000\uE8AB" + //  3880 -  3889
                "\uDEE2\u0000\u0000\uBCDB\u0000\u0000\u0000\uBCDC\u0000\u0000" + //  3890 -  3899
                "\uDCAE\u0000\u0000\u0000\u0000\u0000\u0000\uA1C5\uA1F1\u0000" + //  3900 -  3909
                "\u0000\uD3C6\u0000\uDBE6\u0000\u0000\u0000\u0000\uC8E2\u0000" + //  3910 -  3919
                "\u0000\uC8E3\uC4A1\uC4A2\u0000\u0000\uC4A3\u0000\u0000\uC4A4" + //  3920 -  3929
                "\uC3FB\uC3FC\u0000\uC3FD\u0000\uC3FE\u0000\u0000\uBCA7\u0000" + //  3930 -  3939
                "\u0000\u0000\u0000\u0000\u0000\uD6BB\uDED6\u0000\u0000\uEAA6" + //  3940 -  3949
                "\u0000\u0000\u0000\u0000\u0000\u0000\uC8A2\u0000\u0000\uDEEE" + //  3950 -  3959
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF0E8\u0000\uDDC0\uC3FA" + //  3960 -  3969
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uDBA5\u0000\uCFF7" + //  3970 -  3979
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCDD9\uC3F7\uC3F8" + //  3980 -  3989
                "\u0000\u0000\uC3F9\u0000\u0000\u0000\uD7EE\uD1F1\u0000\u0000" + //  3990 -  3999
                "\u0000\u0000\uD0F0\u0000\u0000\u0000\u0000\uC0CC\uC0CD\u0000" + //  4000 -  4009
                "\u0000\uBBD9\u0000\u0000\u0000\u0000\u0000\u0000\uD5E6\u0000" + //  4010 -  4019
                "\u0000\u0000\uFDFD\u0000\u0000\u0000\u0000\u0000\uB3FA\u0000" + //  4020 -  4029
                "\u0000\u0000\uECD1\u0000\u0000\u0000\u0000\u0000\uFBDD\u0000" + //  4030 -  4039
                "\uEFCA\u0000\uDAEB\u0000\uE2D8\uEDD6\u0000\u0000\uD6D1\uE0B3" + //  4040 -  4049
                "\uC3F3\u0000\u0000\u0000\uC3F4\u0000\u0000\u0000\uD3E9\uE2C9" + //  4050 -  4059
                "\u0000\uFCDB\uCDAD\u0000\uEFC2\u0000\u0000\u0000\u0000\u0000" + //  4060 -  4069
                "\u0000\uEDEC\uC3EE\uC3EF\u0000\uC3F0\u0000\uC3F1\u0000\u0000" + //  4070 -  4079
                "\uBBD7\u0000\u0000\u0000\u0000\u0000\u0000\uEDE4\u0000\u0000" + //  4080 -  4089
                "\uDDE7\uC3ED\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE2CF" + //  4090 -  4099
                "\u0000\uD8AE\u0000\uF9D3\uD5FE\u0000\u0000\u0000\u0000\uC4EC" + //  4100 -  4109
                "\u0000\u0000\u0000\uD7F6\u0000\u0000\u0000\uE2CA\uC3EB\u0000" + //  4110 -  4119
                "\u0000\u0000\uC3EC\u0000\u0000\u0000\uDBD6\u0000\u0000\u0000" + //  4120 -  4129
                "\u0000\u0000\uB9B3\uB9B4\u0000\uB9B5\uC3EA\u0000\u0000\u0000" + //  4130 -  4139
                "\u0000\u0000\u0000\u0000\uCEA8\u0000\uFBCA\u0000\u0000\u0000" + //  4140 -  4149
                "\u0000\u0000\uCDE3\uD8BB\uC3E7\u0000\u0000\u0000\u0000\u0000" + //  4150 -  4159
                "\u0000\u0000\uF4C7\u0000\uEAB9\u0000\u0000\u0000\u0000\u0000" + //  4160 -  4169
                "\uF1DE\u0000\uA4D8\uA4D9\uA4DA\uA4DB\uA4DC\uA4DD\uA4DE\uA4DF" + //  4170 -  4179
                "\uC3E1\u0000\u0000\u0000\uC3E2\u0000\u0000\u0000\uDBC1\u0000" + //  4180 -  4189
                "\u0000\u0000\u0000\u0000\uB9AB\uB9AC\uB9AD\u0000\uDFCF\u0000" + //  4190 -  4199
                "\u0000\uD3C0\uE3D7\u0000\uEFE6\uFCD0\uC3DE\u0000\u0000\u0000" + //  4200 -  4209
                "\u0000\u0000\u0000\u0000\uF3A1\u0000\uEAFD\u0000\uD9DD\u0000" + //  4210 -  4219
                "\uDAB4\uEEAA\uFBE9\u0000\uA4D0\uA4D1\uA4D2\uA4D3\uA4D4\uA4D5" + //  4220 -  4229
                "\uA4D6\uA4D7\uC3DD\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  4230 -  4239
                "\uE9CB\u0000\uCBE2\uD4A4\u0000\uDEE0\uDAFD\uE4C6\uE8BE\u0000" + //  4240 -  4249
                "\uA4C8\uA4C9\uA4CA\uA4CB\uA4CC\uA4CD\uA4CE\uA4CF\uC3D7\u0000" + //  4250 -  4259
                "\u0000\u0000\uC3D8\u0000\u0000\u0000\uFCC4\u0000\u0000\u0000" + //  4260 -  4269
                "\u0000\u0000\uB8FA\u0000\u0000\u0000\uDEAE\u0000\u0000\u0000" + //  4270 -  4279
                "\u0000\u0000\uE4C8\u0000\u0000\u0000\uCCE8\u0000\u0000\u0000" + //  4280 -  4289
                "\u0000\uC7C1\u0000\u0000\u0000\uF6C6\u0000\u0000\u0000\u0000" + //  4290 -  4299
                "\uC7CB\uC7CC\u0000\uC7CD\uC3D3\u0000\u0000\u0000\uC3D4\u0000" + //  4300 -  4309
                "\u0000\u0000\uF5E0\u0000\u0000\u0000\u0000\u0000\uB8E2\uB8E3" + //  4310 -  4319
                "\u0000\uB8E4\uC3CE\uC3CF\u0000\uC3D0\u0000\uC3D1\u0000\u0000" + //  4320 -  4329
                "\uBBBF\u0000\u0000\u0000\u0000\u0000\u0000\uFDF1\u0000\u0000" + //  4330 -  4339
                "\u0000\uF1F9\u0000\uF2C4\uE0CB\u0000\u0000\uFAC5\u0000\u0000" + //  4340 -  4349
                "\u0000\uF9B8\u0000\u0000\uF5D7\u0000\u0000\uD8BF\u0000\u0000" + //  4350 -  4359
                "\u0000\uD1D3\u0000\uE5F0\u0000\u0000\u0000\uE0DE\uF6B4\uEAD2" + //  4360 -  4369
                "\u0000\uF9FB\uC3CD\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  4370 -  4379
                "\uEBB9\u0000\uF4E8\uE5F4\uF4BC\uF4D5\u0000\u0000\u0000\u0000" + //  4380 -  4389
                "\uC4DE\uC4DF\u0000\uC4E0\uC3CA\uC3CB\u0000\u0000\uC3CC\u0000" + //  4390 -  4399
                "\u0000\u0000\uF8E8\u0000\u0000\u0000\u0000\u0000\uB8C9\uB8CA" + //  4400 -  4409
                "\u0000\uB8CB\uC3C8\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  4410 -  4419
                "\uD0B7\u0000\uEFE4\u0000\uD7C5\uEBE2\u0000\u0000\uFCE7\u0000" + //  4420 -  4429
                "\uA4C0\uA4C1\uA4C2\uA4C3\uA4C4\uA4C5\uA4C6\uA4C7\uC3C4\u0000" + //  4430 -  4439
                "\u0000\u0000\uC3C5\u0000\u0000\u0000\uDBA4\u0000\uCFC9\uE2FC" + //  4440 -  4449
                "\uEFFA\u0000\uD4A3\uF0F8\uD7A8\u0000\u0000\u0000\uE1E7\u0000" + //  4450 -  4459
                "\uA4B8\uA4B9\uA4BA\uA4BB\uA4BC\uA4BD\uA4BE\uA4BF\uC3BE\u0000" + //  4460 -  4469
                "\u0000\u0000\uC3BF\u0000\u0000\u0000\uE9E9\u0000\u0000\u0000" + //  4470 -  4479
                "\u0000\u0000\uB8AE\uB8AF\u0000\u0000\uBAF9\uBAFA\uBAFB\u0000" + //  4480 -  4489
                "\u0000\u0000\u0000\uF9A9\u0000\u0000\u0000\u0000\uC2CD\uC2CE" + //  4490 -  4499
                "\u0000\uC2CF\uC3B7\uC3B8\u0000\uC3B9\uC3BA\uC3BB\u0000\u0000" + //  4500 -  4509
                "\uBAE3\u0000\u0000\u0000\u0000\u0000\u0000\uDDA3\u0000\u0000" + //  4510 -  4519
                "\u0000\uF3C2\u0000\u0000\u0000\u0000\u0000\uB2E4\u0000\u0000" + //  4520 -  4529
                "\u0000\uA1D2\u0000\u0000\u0000\u0000\u0000\uDADD\u0000\u0000" + //  4530 -  4539
                "\uDAB9\uC3B6\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD5CD" + //  4540 -  4549
                "\u0000\uEFE3\u0000\u0000\uCFEE\uF6BE\uE0B2\uFCFE\uD1AB\uC3B3" + //  4550 -  4559
                "\uC3B4\u0000\u0000\uC3B5\u0000\u0000\u0000\uE1A5\u0000\u0000" + //  4560 -  4569
                "\u0000\u0000\u0000\uB8A7\uB8A8\u0000\uB8A9\uC3B1\u0000\u0000" + //  4570 -  4579
                "\u0000\u0000\uC3B2\u0000\u0000\uBAD9\uBADA\u0000\uBADB\u0000" + //  4580 -  4589
                "\u0000\u0000\uF3B0\u0000\u0000\uCCC4\u0000\u0000\uDABC\uD8FC" + //  4590 -  4599
                "\u0000\u0000\u0000\u0000\u0000\uB9C5\u0000\u0000\uB9C6\uC3B0" + //  4600 -  4609
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF4F5\u0000\uE1BC" + //  4610 -  4619
                "\uE0EF\u0000\u0000\uE9BF\uFCFD\uE6CE\u0000\uA4B0\uA4B1\uA4B2" + //  4620 -  4629
                "\uA4B3\uA4B4\uA4B5\uA4B6\uA4B7\uC3AD\u0000\u0000\u0000\uC3AE" + //  4630 -  4639
                "\u0000\uC3AF\u0000\uF6D9\uFAF4\u0000\u0000\u0000\u0000\u0000" + //  4640 -  4649
                "\uF8AA\uC3AB\uC3AC\u0000\u0000\u0000\u0000\u0000\u0000\uEED9" + //  4650 -  4659
                "\u0000\u0000\u0000\uF5B2\u0000\u0000\u0000\u0000\u0000\uB8A3" + //  4660 -  4669
                "\uB8A4\u0000\u0000\uBAC0\u0000\u0000\u0000\u0000\u0000\u0000" + //  4670 -  4679
                "\uE3B8\u0000\u0000\u0000\uD0AA\u0000\u0000\u0000\u0000\u0000" + //  4680 -  4689
                "\uB2D8\u0000\u0000\u0000\uA1EE\u0000\u0000\uA1F0\uA1C4\u0000" + //  4690 -  4699
                "\uCEC8\uEAB7\u0000\uFCC0\u0000\uFDE7\uF7EF\u0000\uA4A8\uA4A9" + //  4700 -  4709
                "\uA4AA\uA4AB\uA4AC\uA4AD\uA4AE\uA4AF\uC3A6\u0000\u0000\u0000" + //  4710 -  4719
                "\uC3A7\u0000\u0000\u0000\uF1D0\u0000\u0000\u0000\u0000\u0000" + //  4720 -  4729
                "\uB7F6\u0000\u0000\uB7F7\uC2FC\uC2FD\u0000\uC2FE\uC3A1\uC3A2" + //  4730 -  4739
                "\uC3A3\u0000\uF7C1\u0000\u0000\uE7B6\u0000\u0000\u0000\u0000" + //  4740 -  4749
                "\uC4DA\uC4DB\u0000\u0000\uE2E6\uE2A8\u0000\u0000\u0000\u0000" + //  4750 -  4759
                "\u0000\uDABD\u0000\u0000\u0000\uE7E5\u0000\u0000\uCFCA\uE1D1" + //  4760 -  4769
                "\uC2FB\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCABC\u0000" + //  4770 -  4779
                "\uF1BC\u0000\u0000\uFADA\u0000\u0000\uDAEA\uDAC6\uC2F7\uC2F8" + //  4780 -  4789
                "\u0000\u0000\uC2F9\u0000\uC2FA\u0000\uDDF3\uEAFA\u0000\uF6BD" + //  4790 -  4799
                "\uE1BB\uCDBF\uF4D4\uE6CD\uC2F0\u0000\u0000\u0000\uC2F1\u0000" + //  4800 -  4809
                "\u0000\u0000\uD3B3\u0000\u0000\u0000\u0000\u0000\uB7F2\uB7F3" + //  4810 -  4819
                "\u0000\u0000\uBAA1\uBAA2\u0000\u0000\u0000\u0000\u0000\uBCAD" + //  4820 -  4829
                "\uBCAE\uBCAF\uBCB0\uC2E9\u0000\u0000\u0000\u0000\u0000\u0000" + //  4830 -  4839
                "\u0000\uE7CA\u0000\uD9CB\u0000\uD9D2\uD3CB\uD8F7\uDAA9\uF5F8" + //  4840 -  4849
                "\u0000\uABF0\uABF1\uABF2\uABF3\uABF4\uABF5\uABF6\u0000\u0088" + //  4850 -  4859
                "\u0089\u008A\u008B\u008C\u008D\u0000\u0000\uF0A5\uCBFD\u0000" + //  4860 -  4869
                "\u0000\u0000\u0000\u0000\uE7E0\uEBAE\u0000\u0000\uF0BE\uD2BD" + //  4870 -  4879
                "\uCCA4\u0000\u0000\u0000\u0000\uBEF6\uBEF7\uBEF8\uBEF9\uC2E6" + //  4880 -  4889
                "\uC2E7\u0000\u0000\u0000\u0000\u0000\u0000\uE9B1\u0000\u0000" + //  4890 -  4899
                "\uFAAD\uC2E2\uC2E3\u0000\u0000\u0000\uC2E4\u0000\u0000\uB9F1" + //  4900 -  4909
                "\u0000\u0000\uB9F2\uB9F3\u0000\u0000\uE4AA\u0000\uF5E1\uEDDA" + //  4910 -  4919
                "\u0000\u0000\u0000\uDFC9\u0000\u0000\u0000\u0000\uDFED\uC2E1" + //  4920 -  4929
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD6A6\uDCBE\uC2DE" + //  4930 -  4939
                "\uC2DF\u0000\u0000\uC2E0\u0000\u0000\u0000\uF5DB\u0000\u0000" + //  4940 -  4949
                "\uFAC1\u0000\u0000\uB9E6\u0000\u0000\u0000\uB9E7\u0000\u0000" + //  4950 -  4959
                "\uEBDF\u0000\u0000\u0000\u0000\u0000\uD6CB\uC2DB\uC2DC\u0000" + //  4960 -  4969
                "\u0000\u0000\u0000\u0000\u0000\uCAB0\u0000\uF7A7\u0000\uF6D8" + //  4970 -  4979
                "\u0000\u0000\u0000\uD4C7\u0000\u0000\u0000\uFBAF\u0000\u0000" + //  4980 -  4989
                "\u0000\uCBD1\uC2DA\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  4990 -  4999
                "\uCBE6\u0000\uF7C0\u0000\uD0E3\u0000\u0000\u0000\uDAA1\u0000" + //  5000 -  5009
                "\uABE8\uABE9\uABEA\uABEB\uABEC\uABED\uABEE\uABEF\uC2D8\u0000" + //  5010 -  5019
                "\u0000\u0000\uC2D9\u0000\u0000\u0000\uE4C7\u0000\u0000\u0000" + //  5020 -  5029
                "\u0000\u0000\uB7EF\u0000\u0000\u0000\uF7AC\uEBC4\uEDE1\uE0AB" + //  5030 -  5039
                "\uDDC7\u0000\uCDE7\u0000\uE8DC\u0000\u0000\uE7D7\u0000\u0000" + //  5040 -  5049
                "\uDFCE\u0000\u0000\u0000\u0000\u0000\u0000\uF7B0\u0000\uCCEA" + //  5050 -  5059
                "\uC2D7\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF7C5\u0000" + //  5060 -  5069
                "\uF2AE\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCAD4\uC2D4" + //  5070 -  5079
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCCED\u0000\uE0B1" + //  5080 -  5089
                "\u0000\u0000\u0000\u0000\uDFA5\u0000\uF9D2\uC2D2\uC2D3\u0000" + //  5090 -  5099
                "\u0000\u0000\u0000\u0000\u0000\uD5B1\u0000\u0000\u0000\uCEF4" + //  5100 -  5109
                "\u0000\u0000\u0000\u0000\u0000\uB7E1\u0000\u0000\u0000\uFAA8" + //  5110 -  5119
                "\u0000\u0000\u0000\u0000\u0000\uD6C2\u0000\u0000\u0000\uEFEC" + //  5120 -  5129
                "\u0000\u0000\u0000\u0000\uC6A9\u0000\u0000\u0000\uF4E2\u0000" + //  5130 -  5139
                "\u0000\u0000\u0000\uC6AC\u0000\u0000\u0000\uF2D9\u0000\u0000" + //  5140 -  5149
                "\u0000\u0000\uC6B7\u0000\u0000\u0000\uF4D7\uCCA1\u0000\u0000" + //  5150 -  5159
                "\uCFBA\uC2CB\u0000\u0000\u0000\uC2CC\u0000\u0000\u0000\uFBAE" + //  5160 -  5169
                "\uD1E1\u0000\u0000\uDBC0\u0000\uEFE2\uF1F0\uCFB4\u0000\u0000" + //  5170 -  5179
                "\u0000\u0000\u0000\uCFCF\u0000\u0000\u0000\uD3B4\u0000\u0000" + //  5180 -  5189
                "\u0000\u0000\uFAA6\u0000\u0000\u0000\u0000\uBFFA\uBFFB\u0000" + //  5190 -  5199
                "\u0000\uD9BC\u0000\uE5C6\u0000\u0000\u0000\u0000\uC3C6\u0000" + //  5200 -  5209
                "\u0000\u0000\uD4AF\u0000\u0000\u0000\u0000\uDAE4\u0000\u0000" + //  5210 -  5219
                "\u0000\u0000\uBFD9\u0000\u0000\uBFDA\uC2C8\u0000\u0000\u0000" + //  5220 -  5229
                "\u0000\u0000\u0000\u0000\uDABA\u0000\uE7B5\u0000\u0000\u0000" + //  5230 -  5239
                "\u0000\u0000\uDBF0\u0000\uABE0\uABE1\uABE2\uABE3\uABE4\uABE5" + //  5240 -  5249
                "\uABE6\uABE7\uC2C5\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  5250 -  5259
                "\uE3A8\u0000\uEEFD\u0000\u0000\u0000\u0000\u0000\u0000\uE4AB" + //  5260 -  5269
                "\uC2C3\uC2C4\u0000\u0000\u0000\u0000\u0000\u0000\uFCE6\u0000" + //  5270 -  5279
                "\u0000\u0000\uF2C0\u0000\u0000\uF1E5\u0000\uF4C3\uC2BE\u0000" + //  5280 -  5289
                "\u0000\u0000\uC2BF\u0000\u0000\u0000\uEBB4\uEAA1\u0000\uF8BC" + //  5290 -  5299
                "\uCEA6\u0000\uDDF2\u0000\u0000\uD9BE\u0000\u0000\u0000\u0000" + //  5300 -  5309
                "\uC4AF\uC4B0\u0000\uC4B1\uC2BA\u0000\u0000\u0000\u0000\u0000" + //  5310 -  5319
                "\u0000\u0000\uCDE5\u0000\uEDD1\u0000\u0000\u0000\u0000\u0000" + //  5320 -  5329
                "\uE9F9\u0000\uABD8\uABD9\uABDA\uABDB\uABDC\uABDD\uABDE\uABDF" + //  5330 -  5339
                "\uC2B4\uC2B5\u0000\uC2B6\uC2B7\uC2B8\u0000\u0000\uB9BC\u0000" + //  5340 -  5349
                "\uB9BD\u0000\u0000\u0000\u0000\uD0C4\u0000\u0000\u0000\uCAD0" + //  5350 -  5359
                "\uC2B3\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD0DA\uD0DB" + //  5360 -  5369
                "\uC2B0\uC2B1\u0000\u0000\uC2B2\u0000\u0000\u0000\uE9E6\u0000" + //  5370 -  5379
                "\u0000\uE3F6\u0000\u0000\uB9B6\u0000\u0000\u0000\uB9B7\u0000" + //  5380 -  5389
                "\uB9B8\uC2AE\uC2AF\u0000\u0000\u0000\u0000\u0000\u0000\uD7B4" + //  5390 -  5399
                "\u0000\u0000\u0000\uD5F9\u0000\u0000\u0000\u0000\u0000\uB7CE" + //  5400 -  5409
                "\uB7CF\u0000\u0000\uB9A9\u0000\uB9AA\u0000\u0000\u0000\u0000" + //  5410 -  5419
                "\uF8F4\u0000\uD9B7\u0000\u0000\uE6F3\u0000\u0000\u0000\u0000" + //  5420 -  5429
                "\u0000\u0000\uC1C5\u0000\uC1C6\uC2A7\u0000\uC2A8\u0000\uC2A9" + //  5430 -  5439
                "\u0000\u0000\uC2AA\uC1FC\uC1FD\u0000\uC1FE\u0000\uC2A1\uC2A2" + //  5440 -  5449
                "\u0000\uCFFD\u0000\u0000\uDEDD\u0000\u0000\u0000\uD9D1\uC1FA" + //  5450 -  5459
                "\u0000\uC1FB\u0000\u0000\u0000\u0000\u0000\uBCF2\uBCF3\u0000" + //  5460 -  5469
                "\uBCF4\uC1F6\uC1F7\u0000\u0000\uC1F8\u0000\u0000\uC1F9\uC1F2" + //  5470 -  5479
                "\uC1F3\u0000\uC1F4\u0000\uC1F5\u0000\u0000\uB8C3\u0000\uB8C4" + //  5480 -  5489
                "\uB8C5\uB8C6\u0000\u0000\uE2C8\u0000\u0000\u0000\u0000\u0000" + //  5490 -  5499
                "\u0000\uBCE1\u0000\u0000\uEABD\uE6FE\u0000\uF7C4\uF5AD\u0000" + //  5500 -  5509
                "\uD9E0\uC1F1\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF9C4" + //  5510 -  5519
                "\u0000\uCFB1\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uDBDA" + //  5520 -  5529
                "\uC1EE\uC1EF\u0000\u0000\uC1F0\u0000\u0000\u0000\uF4E5\uD8C2" + //  5530 -  5539
                "\uDCD0\uCCEE\u0000\u0000\uB8B5\u0000\u0000\u0000\u0000\u0000" + //  5540 -  5549
                "\u0000\uF5CD\u0000\uF1F2\uFAC7\uC1EB\u0000\u0000\u0000\uC1EC" + //  5550 -  5559
                "\u0000\u0000\u0000\uFACD\u0000\u0000\u0000\u0000\u0000\uB7C5" + //  5560 -  5569
                "\uB7C6\u0000\uB7C7\uC1E7\uC1E8\u0000\uC1E9\u0000\u0000\u0000" + //  5570 -  5579
                "\u0000\uD3B5\u0000\u0000\u0000\u0000\uC2EA\u0000\u0000\u0000" + //  5580 -  5589
                "\uF3B3\uE4D8\uCFF9\uCFDA\u0000\u0000\uB8AA\uB8AB\u0000\u0000" + //  5590 -  5599
                "\uB8AC\uB8AD\u0000\uFDE2\uF3AD\u0000\uFDDB\u0000\u0000\u0000" + //  5600 -  5609
                "\u0000\uC4AB\uC4AC\u0000\u0000\uD5DA\u0000\uD7A7\u0000\u0000" + //  5610 -  5619
                "\u0000\uEEC0\uC1E6\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  5620 -  5629
                "\uCAE9\u0000\uD3AA\u0000\u0000\u0000\uCCAC\u0000\u0000\u0000" + //  5630 -  5639
                "\uF1CD\u0000\u0000\u0000\u0000\uCFD6\u0000\uD7F0\u0000\uEBE1" + //  5640 -  5649
                "\uC1E3\uC1E4\u0000\u0000\uC1E5\u0000\u0000\u0000\uD7F5\uE3F3" + //  5650 -  5659
                "\uCFE5\u0000\u0000\u0000\uE7F2\uEDDF\u0000\u0000\uCACB\u0000" + //  5660 -  5669
                "\uD0B9\uD4F2\u0000\u0000\u0000\u0000\u0000\uD1A6\uC1E0\u0000" + //  5670 -  5679
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE4B6\u0000\uE8FA\u0000" + //  5680 -  5689
                "\u0000\uCEE9\u0000\u0000\u0000\u0000\uC3C7\u0000\u0000\u0000" + //  5690 -  5699
                "\uF2E2\u0000\u0000\u0000\u0000\uFDDE\uCAC0\u0000\u0000\u0000" + //  5700 -  5709
                "\uCEF2\u0000\uD6D9\u0000\u0000\uCDBE\u0000\uDAE9\u0000\u0000" + //  5710 -  5719
                "\u0000\u0000\uC3C0\uC3C1\u0000\uC3C2\uC1D8\u0000\u0000\u0000" + //  5720 -  5729
                "\uC1D9\uC1DA\uC1DB\u0000\uDAD4\uE2A7\uFBFC\u0000\u0000\uF1DC" + //  5730 -  5739
                "\u0000\u0000\uD9EA\uF5A2\u0000\u0000\u0000\uD7D1\u0000\uABD0" + //  5740 -  5749
                "\uABD1\uABD2\uABD3\uABD4\uABD5\uABD6\uABD7\uC1D2\uC1D3\u0000" + //  5750 -  5759
                "\u0000\uC1D4\u0000\u0000\u0000\uF1CC\u0000\u0000\uE5B8\u0000" + //  5760 -  5769
                "\u0000\uB7E6\u0000\u0000\u0000\u0000\u0000\u0000\uEDC4\u0000" + //  5770 -  5779
                "\u0000\u0000\uCCE1\u0000\u0000\u0000\u0000\uD6EA\uC1CC\u0000" + //  5780 -  5789
                "\u0000\u0000\uC1CD\u0000\u0000\u0000\uF1A1\u0000\u0000\u0000" + //  5790 -  5799
                "\u0000\u0000\uB7B3\uB7B4\u0000\uB7B5\uC1C8\u0000\u0000\u0000" + //  5800 -  5809
                "\u0000\u0000\u0000\u0000\uFBDB\u0000\uCEAA\u0000\uCBC8\u0000" + //  5810 -  5819
                "\u0000\u0000\u0000\u0000\uCDE6\u0000\u0000\u0000\uF0EB\u0000" + //  5820 -  5829
                "\u0000\u0000\u0000\uCBDB\u0000\u0000\u0000\u0000\uBFC0\uBFC1" + //  5830 -  5839
                "\u0000\u0000\uD4A1\uCEB2\u0000\u0000\u0000\u0000\u0000\uDEF8" + //  5840 -  5849
                "\uF8E9\uE3DE\u0000\uABC8\uABC9\uABCA\uABCB\uABCC\uABCD\uABCE" + //  5850 -  5859
                "\uABCF\uC1BB\uC1BC\u0000\uC1BD\u0000\uC1BE\uC1BF\uC1C0\uC1B9" + //  5860 -  5869
                "\u0000\uC1BA\u0000\u0000\u0000\u0000\u0000\uBCEE\uBCEF\u0000" + //  5870 -  5879
                "\u0000\uB7D5\u0000\u0000\u0000\u0000\u0000\u0000\uE5F1\u0000" + //  5880 -  5889
                "\u0000\u0000\uD4BB\u0000\u0000\u0000\u0000\uFDFA\uC1B6\uC1B7" + //  5890 -  5899
                "\u0000\u0000\uC1B8\u0000\u0000\u0000\uE9C9\u0000\u0000\u0000" + //  5900 -  5909
                "\u0000\uD3CE\uC1B1\uC1B2\u0000\u0000\uC1B3\uC1B4\u0000\u0000" + //  5910 -  5919
                "\uB7CC\u0000\uB7CD\u0000\u0000\u0000\u0000\uF6E1\u0000\u0000" + //  5920 -  5929
                "\u0000\u0000\uC2AB\uC2AC\u0000\uC2AD\uC1B0\u0000\u0000\u0000" + //  5930 -  5939
                "\u0000\u0000\u0000\u0000\uE5EE\uDCA2\uC1AE\u0000\u0000\u0000" + //  5940 -  5949
                "\uC1AF\u0000\u0000\u0000\uDDBA\u0000\u0000\u0000\uF2BF\u0000" + //  5950 -  5959
                "\uCED9\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCDBD\uC1A8" + //  5960 -  5969
                "\u0000\u0000\u0000\uC1A9\u0000\u0000\u0000\uEAC6\u0000\u0000" + //  5970 -  5979
                "\u0000\u0000\u0000\uB7AF\uB7B0\u0000\u0000\uB7AE\u0000\u0000" + //  5980 -  5989
                "\u0000\u0000\u0000\u0000\uDDC4\u0000\u0000\u0000\uFBA7\u0000" + //  5990 -  5999
                "\uE9C2\u0000\u0000\u0000\uDEAB\uDBE8\u0000\u0000\uE3DD\uC1A1" + //  6000 -  6009
                "\uC1A2\u0000\uC1A3\u0000\uC1A4\uC1A5\u0000\uF5D1\uE7B3\u0000" + //  6010 -  6019
                "\u0000\u0000\u0000\u0000\u0000\uD1BE\u0000\u0000\uD8F6\u0000" + //  6020 -  6029
                "\uD1A4\u0000\uCDE2\u0000\u0000\uD5D9\u0000\u0000\u0000\uD8DE" + //  6030 -  6039
                "\u0000\u0000\uCEE8\uDBDB\u0000\u0000\u0000\u0000\u0000\uF8FD" + //  6040 -  6049
                "\u0000\u0000\uF8FC\uC0FD\u0000\uC0FE\u0000\u0000\u0000\u0000" + //  6050 -  6059
                "\u0000\uBCE5\u0000\u0000\uBCE6\uC0FA\uC0FB\u0000\u0000\uC0FC" + //  6060 -  6069
                "\u0000\u0000\u0000\uF2F5\u0000\u0000\uD4AE\u0000\u0000\uB6D7" + //  6070 -  6079
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCFA5\u0000\u0000\uDFC4" + //  6080 -  6089
                "\uC0F8\u0000\u0000\u0000\uC0F9\u0000\u0000\u0000\uD6F2\u0000" + //  6090 -  6099
                "\uDEF4\u0000\uDFDB\u0000\uEFDF\u0000\u0000\uF1EF\u0000\uE5F6" + //  6100 -  6109
                "\uEEBF\uE2E4\uC0F5\u0000\u0000\u0000\u0000\uC0F6\u0000\u0000" + //  6110 -  6119
                "\uB5EE\u0000\u0000\u0000\u0000\u0000\u0000\uF6F2\u0000\uDFC2" + //  6120 -  6129
                "\u0000\uD1F8\uEAF8\uEAF9\uDAB3\u0000\u0000\u0000\u0000\uC3A8" + //  6130 -  6139
                "\uC3A9\u0000\uC3AA\uC0F4\u0000\u0000\u0000\u0000\u0000\u0000" + //  6140 -  6149
                "\u0000\uF4C0\u0000\uE5AB\u0000\u0000\u0000\u0000\u0000\u0000" + //  6150 -  6159
                "\u0000\uDDED\uC0F0\uC0F1\u0000\u0000\uC0F2\u0000\uC0F3\u0000" + //  6160 -  6169
                "\uE8CA\u0000\u0000\u0000\u0000\uEBF5\u0000\u0000\uE8C9\uF4FE" + //  6170 -  6179
                "\u0000\u0000\u0000\u0000\uE7FC\uC0EE\uC0EF\u0000\u0000\u0000" + //  6180 -  6189
                "\u0000\u0000\u0000\uE7A5\u0000\uD5F5\uD3BE\uC0E9\u0000\u0000" + //  6190 -  6199
                "\u0000\uC0EA\u0000\u0000\u0000\uD8E7\u0000\uD9C9\u0000\u0000" + //  6200 -  6209
                "\u0000\uCED4\uE7AB\u0000\u0000\u0000\uCBC3\uC0E1\uC0E2\u0000" + //  6210 -  6219
                "\uC0E3\uC0E4\uC0E5\uC0E6\u0000\uCDA1\u0000\u0000\u0000\u0000" + //  6220 -  6229
                "\u0000\uDFB5\u0000\uABC0\uABC1\uABC2\uABC3\uABC4\uABC5\uABC6" + //  6230 -  6239
                "\uABC7\uC0DF\u0000\uC0E0\u0000\u0000\u0000\u0000\u0000\uBCE2" + //  6240 -  6249
                "\u0000\u0000\u0000\uF5B0\u0000\u0000\u0000\u0000\u0000\uB6F7" + //  6250 -  6259
                "\uB6F8\u0000\uB6F9\uC0DA\uC0DB\u0000\u0000\uC0DC\u0000\uC0DD" + //  6260 -  6269
                "\uC0DE\uC0D6\uC0D7\uC0D8\u0000\u0000\u0000\uC0D9\u0000\uD3D6" + //  6270 -  6279
                "\u0000\u0000\u0000\u0000\u0000\u0000\uEDD0\uC0CE\u0000\u0000" + //  6280 -  6289
                "\u0000\uC0CF\uC0D0\uC0D1\u0000\uD0BE\u0000\uDDDC\u0000\u0000" + //  6290 -  6299
                "\u0000\u0000\uD4D6\uC0CA\u0000\u0000\uC0CB\u0000\u0000\u0000" + //  6300 -  6309
                "\u0000\uD9E8\u0000\uF7EB\uF5C9\u0000\uCCFE\uCDE1\u0000\uE1BA" + //  6310 -  6319
                "\u0000\uDBEF\uDAB2\u0000\uABB8\uABB9\uABBA\uABBB\uABBC\uABBD" + //  6320 -  6329
                "\uABBE\uABBF\uC0C9\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  6330 -  6339
                "\uCCEC\u0000\uD7DE\u0000\u0000\u0000\u0000\uDEDC\u0000\uF0AC" + //  6340 -  6349
                "\uC0C7\u0000\u0000\u0000\uC0C8\u0000\u0000\u0000\uFBDF\uE7E3" + //  6350 -  6359
                "\u0000\u0000\u0000\u0000\uFCF1\u0000\u0000\u0000\uD0BC\uC0BA" + //  6360 -  6369
                "\u0000\u0000\u0000\uC0BB\u0000\u0000\u0000\uD4CA\u0000\u0000" + //  6370 -  6379
                "\u0000\u0000\u0000\uB6EA\uB6EB\u0000\u0000\uB5DF\u0000\u0000" + //  6380 -  6389
                "\u0000\u0000\u0000\u0000\uD8C5\u0000\u0000\u0000\uF1BD\u0000" + //  6390 -  6399
                "\u0000\uE2E7\uFDD7\u0000\uD4D5\uDFCD\u0000\uFCB8\uD1D0\u0000" + //  6400 -  6409
                "\u0000\u0000\uDDB8\uCFC5\uDFDF\u0000\u0000\uCFAF\u0000\u0000" + //  6410 -  6419
                "\uCAD3\u0000\u0000\uCAAF\uC0B3\uC0B4\u0000\uC0B5\u0000\uC0B6" + //  6420 -  6429
                "\u0000\uC0B7\uC0B2\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  6430 -  6439
                "\uCADD\uD5DE\uC0AF\uC0B0\u0000\u0000\uC0B1\u0000\u0000\u0000" + //  6440 -  6449
                "\uF3C6\u0000\u0000\u0000\u0000\u0000\uB6E7\u0000\u0000\u0000" + //  6450 -  6459
                "\uCAED\u0000\u0000\u0000\u0000\uE8EB\uC0A9\u0000\u0000\u0000" + //  6460 -  6469
                "\uC0AA\u0000\u0000\u0000\uDCA8\u0000\u0000\u0000\u0000\u0000" + //  6470 -  6479
                "\uB6D8\u0000\u0000\u0000\uE1F7\u0000\u0000\u0000\u0000\u0000" + //  6480 -  6489
                "\uF5ED\u0000\uCFF3\u0000\uE4D0\u0000\u0000\u0000\u0000\u0000" + //  6490 -  6499
                "\uF2EE\u0000\uABB0\uABB1\uABB2\uABB3\uABB4\uABB5\uABB6\uABB7" + //  6500 -  6509
                "\uC0A4\uC0A5\u0000\u0000\u0000\uC0A6\u0000\u0000\uB5BF\u0000" + //  6510 -  6519
                "\uB5C0\u0000\uB5C1\u0000\u0000\uF5EC\u0000\u0000\u0000\u0000" + //  6520 -  6529
                "\u0000\uEEE7\uC0A3\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  6530 -  6539
                "\uF5AB\u0000\uCBFA\uF9F9\uCCFD\uD3FE\u0000\u0000\u0000\u0000" + //  6540 -  6549
                "\uC3A4\uC3A5\u0000\u0000\uE8DB\u0000\uDBB3\u0000\u0000\u0000" + //  6550 -  6559
                "\uD1F7\uBFFE\uC0A1\u0000\u0000\uC0A2\u0000\u0000\u0000\uEFF5" + //  6560 -  6569
                "\uCADF\u0000\uEBB1\uEDBF\u0000\uFAB7\uD0C6\u0000\u0000\uCCAB" + //  6570 -  6579
                "\uEEA8\u0000\u0000\uDFB4\u0000\u0000\u0000\u0000\uD7DD\uFABA" + //  6580 -  6589
                "\uBFFC\uBFFD\u0000\u0000\u0000\u0000\u0000\u0000\uE7FB\uFCB7" + //  6590 -  6599
                "\uFCE4\uFBC5\uBFF8\u0000\u0000\u0000\uBFF9\u0000\u0000\u0000" + //  6600 -  6609
                "\uD5DF\u0000\u0000\u0000\uD6E5\u0000\uD2B0\uF1BA\u0000\uD7B3" + //  6610 -  6619
                "\uE3C3\uF3FD\uDEDA\u0000\uABA8\uABA9\uABAA\uABAB\uABAC\uABAD" + //  6620 -  6629
                "\uABAE\uABAF\uBFF2\uBFF3\u0000\uBFF4\u0000\uBFF5\u0000\u0000" + //  6630 -  6639
                "\uB5A2\u0000\uB5A3\u0000\u0000\uB5A4\u0000\uD9BB\uCAF3\uF6D3" + //  6640 -  6649
                "\uE6F8\uEAF5\u0000\u0000\u0000\uEBB0\uF4E3\u0000\u0000\u0000" + //  6650 -  6659
                "\uCFC4\u0000\u0000\u0000\u0000\uCBA7\u0000\uDACE\u0000\u0000" + //  6660 -  6669
                "\uF1B9\u0000\u0000\uDAD3\u0000\uF6EA\u0000\uAAF0\uAAF1\uAAF2" + //  6670 -  6679
                "\uAAF3\u0000\u0000\u0000\u0000\uBEBA\u0000\u0000\u0000\uD6D0" + //  6680 -  6689
                "\u0000\u0000\u0000\u0000\uC3E9\u0000\u0000\u0000\uE5A1\u0000" + //  6690 -  6699
                "\u0000\u0000\u0000\uC3F5\u0000\u0000\u0000\uE4EA\uF2CF\u0000" + //  6700 -  6709
                "\uF7BF\u0000\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087" + //  6710 -  6719
                "\uBFEF\uBFF0\uBFF1\u0000\u0000\u0000\u0000\u0000\uBCD8\uBCD9" + //  6720 -  6729
                "\u0000\uBCDA\uBFEC\uBFED\u0000\u0000\uBFEE\u0000\u0000\u0000" + //  6730 -  6739
                "\uF4DB\u0000\uE2F4\u0000\u0000\uD3C8\uBFE6\u0000\u0000\u0000" + //  6740 -  6749
                "\uBFE7\u0000\u0000\u0000\uDBBE\u0000\u0000\uE0E2\u0000\u0000" + //  6750 -  6759
                "\uB4BF\u0000\u0000\u0000\u0000\u0000\u0000\uF6E3\u0000\u0000" + //  6760 -  6769
                "\u0000\uCBFE\uEDEA\u0000\u0000\u0000\u0000\uC3D9\uC3DA\u0000" + //  6770 -  6779
                "\uC3DB\uBFE0\uBFE1\u0000\uBFE2\u0000\uBFE3\u0000\u0000\uB4B1" + //  6780 -  6789
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE2D2\u0000\uF6A2\uE1F4" + //  6790 -  6799
                "\uBFDF\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE1A1\u0000" + //  6800 -  6809
                "\uCFAE\u0000\u0000\u0000\u0000\u0000\uE3C2\u0000\uAAE8\uAAE9" + //  6810 -  6819
                "\uAAEA\uAAEB\uAAEC\uAAED\uAAEE\uAAEF\uBFDC\uBFDD\u0000\u0000" + //  6820 -  6829
                "\uBFDE\u0000\u0000\u0000\uCAC7\u0000\u0000\u0000\u0000\u0000" + //  6830 -  6839
                "\uB6CD\u0000\u0000\u0000\uD2A5\u0000\u0000\uFDEE\u0000\u0000" + //  6840 -  6849
                "\uB4A6\u0000\uB4A7\u0000\uB4A8\u0000\u0000\uF1FB\u0000\u0000" + //  6850 -  6859
                "\u0000\u0000\uFDD2\uD1C1\uBFD8\u0000\u0000\u0000\u0000\u0000" + //  6860 -  6869
                "\u0000\u0000\uDDAE\u0000\uF8C2\u0000\u0000\uF2AC\u0000\u0000" + //  6870 -  6879
                "\uCAAD\uCAAE\uBFD1\uBFD2\u0000\uBFD3\uBFD4\uBFD5\u0000\u0000" + //  6880 -  6889
                "\uB3A9\u0000\u0000\u0000\u0000\u0000\u0000\uF7F7\uDCAC\u0000" + //  6890 -  6899
                "\u0000\uCCAF\u0000\u0000\u0000\u0000\u0000\u0000\uB0EB\u0000" + //  6900 -  6909
                "\uB0EC\uBFD0\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE9A4" + //  6910 -  6919
                "\u0000\uE0B0\u0000\u0000\uD4E2\u0000\uF6D7\u0000\uD7F9\uBFCD" + //  6920 -  6929
                "\uBFCE\u0000\u0000\uBFCF\u0000\u0000\u0000\uCAC6\u0000\u0000" + //  6930 -  6939
                "\uD5C2\u0000\u0000\uB2FE\u0000\u0000\u0000\uB3A1\u0000\u0000" + //  6940 -  6949
                "\uE9A9\u0000\uD3C7\u0000\u0000\uDCDD\uF8AE\uBFC6\u0000\u0000" + //  6950 -  6959
                "\uBFC7\uBFC8\uBFC9\u0000\uBFCA\uBFC2\u0000\u0000\u0000\uBFC3" + //  6960 -  6969
                "\uBFC4\uBFC5\u0000\uCBF9\uD4D4\u0000\uD9DC\u0000\uEEBE\u0000" + //  6970 -  6979
                "\uF7ED\uBFBC\uBFBD\u0000\uBFBE\uBFBF\u0000\u0000\u0000\uE3A9" + //  6980 -  6989
                "\u0000\u0000\u0000\u0000\u0000\uB6C0\uB6C1\u0000\uB6C2\uBFBB" + //  6990 -  6999
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uDFFD\u0000\uEEA7" + //  7000 -  7009
                "\uF5BD\u0000\uF8F5\u0000\u0000\uEDE8\u0000\uAAE0\uAAE1\uAAE2" + //  7010 -  7019
                "\uAAE3\uAAE4\uAAE5\uAAE6\uAAE7\uBFB9\u0000\u0000\u0000\uBFBA" + //  7020 -  7029
                "\u0000\u0000\u0000\uD8CA\u0000\u0000\u0000\u0000\u0000\uB6AB" + //  7030 -  7039
                "\uB6AC\u0000\uB6AD\uBFB4\uBFB5\u0000\u0000\u0000\uBFB6\uBFB7" + //  7040 -  7049
                "\uBFB8\uBFAC\u0000\u0000\u0000\uBFAD\u0000\uBFAE\uBFAF\uBFA5" + //  7050 -  7059
                "\uBFA6\u0000\uBFA7\u0000\uBFA8\u0000\u0000\uB2D1\u0000\u0000" + //  7060 -  7069
                "\u0000\u0000\u0000\u0000\uF4A2\u0000\uF1D7\u0000\uD9CA\u0000" + //  7070 -  7079
                "\uDAB1\uD8C7\uDCE2\uF3CE\uF5F4\u0000\uAAD8\uAAD9\uAADA\uAADB" + //  7080 -  7089
                "\uAADC\uAADD\uAADE\uAADF\uBFA4\u0000\u0000\u0000\u0000\u0000" + //  7090 -  7099
                "\u0000\u0000\uEFF2\uE2B7\uBFA1\uBFA2\u0000\u0000\uBFA3\u0000" + //  7100 -  7109
                "\u0000\u0000\uF7DB\u0000\u0000\u0000\u0000\u0000\uB5F5\uB5F6" + //  7110 -  7119
                "\u0000\uB5F7\uBEFA\uBEFB\uBEFC\u0000\uBEFD\u0000\uBEFE\u0000" + //  7120 -  7129
                "\uEAC5\uEAF3\u0000\uDDDB\u0000\uDCD7\u0000\u0000\uDFF5\u0000" + //  7130 -  7139
                "\u0000\uE8F8\uF8ED\u0000\u0000\uE0E7\u0000\uCCD9\u0000\u0000" + //  7140 -  7149
                "\uD4C6\u0000\uAAD0\uAAD1\uAAD2\uAAD3\uAAD4\uAAD5\uAAD6\uAAD7" + //  7150 -  7159
                "\uBEF0\uBEF1\u0000\uBEF2\uBEF3\uBEF4\uBEF5\u0000\uF5CC\u0000" + //  7160 -  7169
                "\u0000\uFCE5\u0000\u0000\u0000\u0000\uC2F2\uC2F3\u0000\u0000" + //  7170 -  7179
                "\uFCFB\u0000\u0000\u0000\u0000\u0000\u0000\uD4FE\u0000\u0000" + //  7180 -  7189
                "\uF1DB\u0000\uFAD9\u0000\uF1B8\uFDF5\uE0F9\uBEEC\u0000\u0000" + //  7190 -  7199
                "\u0000\u0000\u0000\u0000\u0000\uCEED\u0000\uDFF6\uF0C7\uF0C6" + //  7200 -  7209
                "\u0000\uD8BA\u0000\uF1F4\uF4F0\uBEEA\u0000\u0000\u0000\uBEEB" + //  7210 -  7219
                "\u0000\u0000\u0000\uFAE5\uE2FA\u0000\u0000\u0000\uCAB6\uBEE1" + //  7220 -  7229
                "\u0000\u0000\u0000\uBEE2\u0000\u0000\uBEE3\uBEDA\uBEDB\u0000" + //  7230 -  7239
                "\uBEDC\uBEDD\uBEDE\u0000\u0000\uB2C7\uB2C8\uB2C9\u0000\u0000" + //  7240 -  7249
                "\u0000\u0000\uDED4\u0000\uE0AC\u0000\u0000\uD7EB\u0000\u0000" + //  7250 -  7259
                "\u0000\u0000\u0000\u0000\uE0A8\uD5F3\u0000\uE8D8\u0000\uCDD8" + //  7260 -  7269
                "\uE7D6\uCCDA\u0000\u0000\uCAE3\uBED9\u0000\u0000\u0000\u0000" + //  7270 -  7279
                "\u0000\u0000\u0000\uDDD0\uF0D9\uBED6\uBED7\u0000\u0000\uBED8" + //  7280 -  7289
                "\u0000\u0000\u0000\uF1E3\uD5EE\u0000\u0000\u0000\u0000\uA1A4" + //  7290 -  7299
                "\u0000\u0000\u0000\u0000\uBEBD\u0000\u0000\u0000\uF4C9\u0000" + //  7300 -  7309
                "\u0000\u0000\u0000\u0000\uDEA7\u0000\u0000\uE6BB\uBED2\uBED3" + //  7310 -  7319
                "\u0000\u0000\u0000\uBED4\uBED5\u0000\uCEC7\u0000\u0000\u0000" + //  7320 -  7329
                "\u0000\u0000\uFDF6\u0000\uAAC8\uAAC9\uAACA\uAACB\uAACC\uAACD" + //  7330 -  7339
                "\uAACE\uAACF\uBEC8\uBEC9\uBECA\u0000\uBECB\uBECC\uBECD\u0000" + //  7340 -  7349
                "\uCDBC\u0000\uF3E5\u0000\u0000\u0000\u0000\u0000\uE4D4\u0000" + //  7350 -  7359
                "\u0000\u0000\uE0A5\u0000\uF7AB\u0000\u0000\uF2CB\u0000\uF2CC" + //  7360 -  7369
                "\u0000\u0000\u0000\uE4CF\uBEC2\uBEC3\u0000\uBEC4\u0000\uBEC5" + //  7370 -  7379
                "\u0000\u0000\uB1C3\uB1C4\u0000\u0000\u0000\u0000\u0000\uBAF1" + //  7380 -  7389
                "\uBAF2\u0000\u0000\uF6C9\u0000\u0000\u0000\u0000\u0000\u0000" + //  7390 -  7399
                "\uD8EA\u0000\u0000\uCEB7\u0000\u0000\u0000\u0000\u0000\u0000" + //  7400 -  7409
                "\uD4EF\u0000\u0000\uE0C4\u0000\uCFB9\u0000\uD5CA\uD7E2\uE2AF" + //  7410 -  7419
                "\uBEC1\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD3D8\u0000" + //  7420 -  7429
                "\uE5C3\u0000\u0000\u0000\u0000\uD9A6\u0000\u0000\uF0C5\uE3C1" + //  7430 -  7439
                "\uFCCC\uFCCD\u0000\u0000\u0000\uEECC\u0000\u0000\uDEF2\u0000" + //  7440 -  7449
                "\uAAC0\uAAC1\uAAC2\uAAC3\uAAC4\uAAC5\uAAC6\uAAC7\uBEBE\uBEBF" + //  7450 -  7459
                "\u0000\u0000\uBEC0\u0000\u0000\u0000\uF5F9\u0000\u0000\u0000" + //  7460 -  7469
                "\u0000\u0000\uB5E5\uB5E6\u0000\u0000\uB1B6\u0000\uB1B7\u0000" + //  7470 -  7479
                "\u0000\u0000\u0000\uDAAE\uCAEE\u0000\u0000\u0000\uCFEC\u0000" + //  7480 -  7489
                "\u0000\u0000\u0000\uF4DF\uBEBB\u0000\u0000\u0000\uBEBC\u0000" + //  7490 -  7499
                "\u0000\u0000\uD5A2\u0000\u0000\u0000\u0000\u0000\uB5DA\u0000" + //  7500 -  7509
                "\u0000\u0000\uD5E5\u0000\u0000\u0000\u0000\u0000\uD5C1\u0000" + //  7510 -  7519
                "\u0000\u0000\uFADB\uCBE3\uF7A9\u0000\uFBA6\uBEB8\uBEB9\u0000" + //  7520 -  7529
                "\u0000\u0000\u0000\u0000\u0000\uF4CC\uDAFC\u0000\u0000\uB1B2" + //  7530 -  7539
                "\u0000\u0000\u0000\u0000\u0000\u0000\uDAE1\u0000\uD6B6\u0000" + //  7540 -  7549
                "\uEAD1\uDFF4\u0000\u0000\u0000\u0000\uD1EC\uE4DE\uBEB5\u0000" + //  7550 -  7559
                "\uBEB6\u0000\u0000\u0000\u0000\uBEB7\uBEB2\uBEB3\u0000\u0000" + //  7560 -  7569
                "\uBEB4\u0000\u0000\u0000\uFBAB\u0000\u0000\u0000\u0000\u0000" + //  7570 -  7579
                "\uB5D6\u0000\u0000\u0000\uDACF\u0000\uDCD4\u0000\uDCA6\u0000" + //  7580 -  7589
                "\uFDBB\uFDC7\u0000\u0000\u0000\u0000\uE7B2\u0000\uAAB8\uAAB9" + //  7590 -  7599
                "\uAABA\uAABB\uAABC\uAABD\uAABE\uAABF\uBEAF\u0000\u0000\u0000" + //  7600 -  7609
                "\uBEB0\u0000\u0000\u0000\uD5CC\u0000\u0000\u0000\u0000\u0000" + //  7610 -  7619
                "\uB5CD\u0000\u0000\u0000\uDDE3\u0000\u0000\u0000\u0000\uFCDD" + //  7620 -  7629
                "\uBEAC\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD5DB\u0000" + //  7630 -  7639
                "\uEFDD\u0000\uF2AA\u0000\u0000\u0000\u0000\u0000\uF0B9\uE4FE" + //  7640 -  7649
                "\uE4C9\u0000\uAAB0\uAAB1\uAAB2\uAAB3\uAAB4\uAAB5\uAAB6\uAAB7" + //  7650 -  7659
                "\uBEA7\u0000\u0000\u0000\uBEA8\u0000\u0000\u0000\uE0D0\u0000" + //  7660 -  7669
                "\u0000\u0000\u0000\u0000\uB5C5\u0000\u0000\u0000\uDEFB\uD0BB" + //  7670 -  7679
                "\uD5B7\uEEF1\u0000\u0000\uB0E7\u0000\u0000\uB0E8\u0000\u0000" + //  7680 -  7689
                "\u0000\uF3A4\u0000\u0000\u0000\uD4FB\uFCE3\uBEA4\u0000\u0000" + //  7690 -  7699
                "\u0000\u0000\u0000\u0000\u0000\uD3AF\u0000\uE3E7\uD8B9\u0000" + //  7700 -  7709
                "\uF6F8\u0000\u0000\uDCC5\uCCD8\uBDFE\u0000\u0000\u0000\uBEA1" + //  7710 -  7719
                "\u0000\u0000\u0000\uE8B3\u0000\u0000\u0000\u0000\u0000\uB5B5" + //  7720 -  7729
                "\uB5B6\u0000\u0000\uB0D1\uB0D2\uB0D3\uB0D4\u0000\u0000\u0000" + //  7730 -  7739
                "\uDDE9\u0000\u0000\u0000\uF1F3\u0000\uF9C0\uE9F0\u0000\u0000" + //  7740 -  7749
                "\uD9DB\u0000\uF3E4\u0000\uAAA8\uAAA9\uAAAA\uAAAB\uAAAC\uAAAD" + //  7750 -  7759
                "\uAAAE\uAAAF\uBDFB\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  7760 -  7769
                "\uF9B6\u0000\uEDC6\u0000\u0000\u0000\u0000\uE1B9\u0000\uE3C0" + //  7770 -  7779
                "\uBDFA\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCCE6\u0000" + //  7780 -  7789
                "\uE7B1\u0000\u0000\u0000\u0000\uF5F0\u0000\uD8DC\uBDF9\u0000" + //  7790 -  7799
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF0FA\u0000\uDDCB\uD0D4" + //  7800 -  7809
                "\u0000\uE6B6\uE0AE\uFDDA\u0000\u0000\uF9D1\u0000\u0000\uE9D9" + //  7810 -  7819
                "\u0000\u0000\u0000\uCCC1\u0000\uD2ED\u0000\u0000\uDBB2\uFBC4" + //  7820 -  7829
                "\u0000\uF3E3\u0000\uD9A5\uFBE7\uBDF4\uBDF5\u0000\u0000\u0000" + //  7830 -  7839
                "\uBDF6\u0000\u0000\uB0B0\uB0B1\uB0B2\uB0B3\uB0B4\u0000\u0000" + //  7840 -  7849
                "\uEDBE\u0000\u0000\u0000\uD5C0\uE3F0\uEDFA\uBDF2\u0000\uBDF3" + //  7850 -  7859
                "\u0000\u0000\u0000\u0000\u0000\uBCD2\uBCD3\uBCD4\u0000\uF2A9" + //  7860 -  7869
                "\uF0C4\u0000\u0000\uE2E2\uE9EF\u0000\u0000\uDBB1\u0000\u0000" + //  7870 -  7879
                "\u0000\uD5E9\u0000\u0000\uDBB0\u0000\u0000\uE5DA\uE3BF\u0000" + //  7880 -  7889
                "\u0000\uE4D6\u0000\u0000\uD0C5\uF4AE\u0000\uDDA8\uBDEE\uBDEF" + //  7890 -  7899
                "\u0000\u0000\uBDF0\u0000\u0000\uBDF1\uBDED\u0000\u0000\u0000" + //  7900 -  7909
                "\u0000\u0000\u0000\u0000\uE5DD\uFDFB\uBDEB\u0000\u0000\u0000" + //  7910 -  7919
                "\uBDEC\u0000\u0000\u0000\uE7DE\u0000\u0000\u0000\uD6D6\uE1CC" + //  7920 -  7929
                "\uBDE6\uBDE7\u0000\u0000\uBDE8\uBDE9\u0000\u0000\uD6C6\u0000" + //  7930 -  7939
                "\u0000\u0000\u0000\u0000\uE0E5\uBDE4\u0000\uBDE5\u0000\u0000" + //  7940 -  7949
                "\u0000\u0000\u0000\uBCC9\uBCCA\u0000\uBCCB\uBDE1\uBDE2\u0000" + //  7950 -  7959
                "\u0000\uBDE3\u0000\u0000\u0000\uD7F4\uF0DD\u0000\u0000\u0000" + //  7960 -  7969
                "\uCEAB\uBDDE\uBDDF\u0000\u0000\u0000\u0000\u0000\u0000\uE1B8" + //  7970 -  7979
                "\u0000\uE8F4\uD3FD\uBDDA\u0000\u0000\u0000\uBDDB\u0000\u0000" + //  7980 -  7989
                "\u0000\uF0B1\u0000\u0000\u0000\u0000\u0000\uB4FD\uB4FE\u0000" + //  7990 -  7999
                "\uB5A1\uBDD3\uBDD4\u0000\u0000\uBDD5\uBDD6\u0000\u0000\uFDD9" + //  8000 -  8009
                "\u0000\u0000\uCCA3\u0000\u0000\u0000\uE1E5\uEEF9\u0000\u0000" + //  8010 -  8019
                "\u0000\uE7F6\uBDD2\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  8020 -  8029
                "\uE6A6\u0000\uD9E9\u0000\u0000\uD0FE\u0000\uECED\uD3A9\u0000" + //  8030 -  8039
                "\uA1BC\uA1BD\u0000\uA1EB\uA1B2\uA1B3\u0000\u0000\uEEF3\uE7F1" + //  8040 -  8049
                "\u0000\uFDB4\u0000\u0000\u0000\uE6B9\u0000\u0000\u0000\u0000" + //  8050 -  8059
                "\uC5F0\u0000\u0000\u0000\uD7AA\u0000\u0000\u0000\u0000\uC5F9" + //  8060 -  8069
                "\uC5FA\u0000\uC5FB\uBDCE\uBDCF\u0000\uBDD0\uBDD1\u0000\u0000" + //  8070 -  8079
                "\u0000\uE2BE\u0000\u0000\u0000\u0000\u0000\uB4F5\uB4F6\uB4F7" + //  8080 -  8089
                "\u0000\uE3E6\u0000\u0000\u0000\u0000\u0000\u0000\uD3A8\uBDC5" + //  8090 -  8099
                "\u0000\u0000\uBDC6\uBDC7\u0000\u0000\u0000\uF8E4\u0000\u0000" + //  8100 -  8109
                "\u0000\u0000\u0000\uB4F4\u0000\u0000\u0000\uF3FC\u0000\u0000" + //  8110 -  8119
                "\uEEA2\u0000\u0000\uF9E9\u0000\u0000\u0000\uE7A4\u0000\uD6E3" + //  8120 -  8129
                "\uBDBC\u0000\u0000\u0000\uBDBD\uBDBE\u0000\u0000\uD9D0\u0000" + //  8130 -  8139
                "\u0000\u0000\u0000\u0000\uE5A3\uBDB7\u0000\u0000\uBDB8\u0000" + //  8140 -  8149
                "\uBDB9\u0000\u0000\uCCBB\u0000\u0000\u0000\u0000\u0000\u0000" + //  8150 -  8159
                "\uA6A3\uA6C8\uA6C7\uA6AE\uBDB6\u0000\u0000\u0000\u0000\u0000" + //  8160 -  8169
                "\u0000\u0000\uF5A8\u0000\uDDEC\uDAE8\u0000\u0000\u0000\u0000" + //  8170 -  8179
                "\u0000\uD4E0\uBDB4\uBDB5\u0000\u0000\u0000\u0000\u0000\u0000" + //  8180 -  8189
                "\uCEC0\uE3D4\uD1CF\uF1F5\uBDAE\u0000\u0000\u0000\uBDAF\u0000" + //  8190 -  8199
                "\u0000\u0000\uF1F1\u0000\u0000\u0000\u0000\u0000\uB4BD\uB4BE" + //  8200 -  8209
                "\u0000\u0000\uE4BC\u0000\u0000\u0000\u0000\u0000\u0000\uA9CD" + //  8210 -  8219
                "\uA9CE\uA9CF\uA9D0\uBDAA\u0000\u0000\u0000\u0000\uBDAB\u0000" + //  8220 -  8229
                "\u0000\uCFCD\u0000\u0000\u0000\u0000\u0000\u0000\uA9E7\uA9E8" + //  8230 -  8239
                "\uA9E9\uA9EA\uBDA9\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  8240 -  8249
                "\uD8BE\u0000\uE2A6\u0000\u0000\u0000\u0000\u0000\uE0C0\u0000" + //  8250 -  8259
                "\uA1B4\uA1B5\uA1B6\uA1B7\uA1B8\uA1B9\uA1BA\uA1BB\uBDA6\uBDA7" + //  8260 -  8269
                "\u0000\u0000\uBDA8\u0000\u0000\u0000\uD5D1\u0000\uD8F0\uF8C3" + //  8270 -  8279
                "\uEAD7\u0000\uFCF9\u0000\u0000\u0000\u0000\uDFF3\uCEE7\uDAC2" + //  8280 -  8289
                "\uBDA5\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF2D8\u0000" + //  8290 -  8299
                "\uEAF2\uCBC7\u0000\uCDF4\u0000\u0000\uDBAF\uEFD9\uBCFB\uBCFC" + //  8300 -  8309
                "\u0000\uBCFD\u0000\uBCFE\u0000\uBDA1\uBCFA\u0000\u0000\u0000" + //  8310 -  8319
                "\u0000\u0000\u0000\u0000\uD9CD\u0000\uE5C1\u0000\u0000\u0000" + //  8320 -  8329
                "\u0000\uE0EE\u0000\u0000\uD6F9\u0000\uCDD7\uDED8\u0000\u0000" + //  8330 -  8339
                "\uF2F8\uBCF6\uBCF7\u0000\u0000\uBCF8\u0000\u0000\uBCF9\uBCF0" + //  8340 -  8349
                "\u0000\u0000\u0000\uBCF1\u0000\u0000\u0000\uD8D3\u0000\u0000" + //  8350 -  8359
                "\u0000\u0000\u0000\uB4B4\u0000\u0000\u0000\uD6F6\u0000\u0000" + //  8360 -  8369
                "\u0000\uEACA\u0000\uDAE7\u0000\u0000\u0000\uF7CC\u0000\u0000" + //  8370 -  8379
                "\u0000\uFDBA\u0000\u0000\uFDE1\uF6FE\uBCEB\uBCEC\u0000\uBCED" + //  8380 -  8389
                "\u0000\u0000\u0000\u0000\uF1A9\u0000\u0000\u0000\uF2C9\uBCEA" + //  8390 -  8399
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF9AB\u0000\uDBFB" + //  8400 -  8409
                "\u0000\uCBE0\u0000\u0000\u0000\u0000\u0000\uD4B7\u0000\u0000" + //  8410 -  8419
                "\u0000\uD8F2\u0000\u0000\uE3CA\u0000\uA1A1\uA1A2\uA1A3\uA1A8" + //  8420 -  8429
                "\u0000\u0000\u0000\u0000\uBEAE\u0000\u0000\u0000\uF9E3\u0000" + //  8430 -  8439
                "\u0000\u0000\u0000\uC3E3\uC3E4\u0000\uC3E5\uBCE8\u0000\u0000" + //  8440 -  8449
                "\u0000\uBCE9\u0000\u0000\u0000\uF9FE\uDBBA\uDAF5\u0000\u0000" + //  8450 -  8459
                "\u0000\uE7EC\uEEEE\u0000\uF3F0\u0000\uDFBF\uBCE7\u0000\u0000" + //  8460 -  8469
                "\u0000\u0000\u0000\u0000\u0000\uCEEB\u0000\uF3BC\u0000\uDAD2" + //  8470 -  8479
                "\u0000\u0000\u0000\u0000\u0000\uFCA8\u0000\u0000\uECE6\uBCE3" + //  8480 -  8489
                "\u0000\u0000\u0000\uBCE4\u0000\u0000\u0000\uE0A2\u0000\u0000" + //  8490 -  8499
                "\u0000\u0000\u0000\uB4B3\u0000\u0000\u0000\uF1AE\u0000\uEFCE" + //  8500 -  8509
                "\u0000\u0000\u0000\uD7FA\u0000\u0000\u0000\uFBC8\uBCE0\u0000" + //  8510 -  8519
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCBB2\uF1C2\uBCDD\uBCDE" + //  8520 -  8529
                "\u0000\u0000\uBCDF\u0000\u0000\u0000\uD3E6\uF2DD\uCFBF\u0000" + //  8530 -  8539
                "\uEBAC\u0000\uD6CE\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + //  8540 -  8549
                "\uD9DA\uBCD5\u0000\u0000\u0000\uBCD6\u0000\uBCD7\u0000\uD9B8" + //  8550 -  8559
                "\uD9B9\uEFC9\u0000\u0000\u0000\u0000\u0000\uF0A1\u0000\uDEAA" + //  8560 -  8569
                "\u0000\uA2CD\uA2DB\uA2DC\u0000\uA2DD\uA2DA\u0000\u0000\uE1B1" + //  8570 -  8579
                "\u0000\u0000\uF7B2\u0000\u0000\uD3F3\uBCD0\u0000\u0000\u0000" + //  8580 -  8589
                "\u0000\u0000\u0000\u0000\uE8B7\u0000\uF5C4\u0000\u0000\u0000" + //  8590 -  8599
                "\u0000\u0000\u0000\u0000\uDCE9\uBCCE\u0000\u0000\u0000\uBCCF" + //  8600 -  8609
                "\u0000\u0000\u0000\uEBEC\u0000\u0000\uD3C5\uFCEC\uD2DB\uBCCC" + //  8610 -  8619
                "\uBCCD\u0000\u0000\u0000\u0000\u0000\u0000\uFCE2\u0000\uF8A7" + //  8620 -  8629
                "\u0000\uEED7\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCED7" + //  8630 -  8639
                "\uBCC7\u0000\u0000\u0000\uBCC8\u0000\u0000\u0000\uDDB1\u0000" + //  8640 -  8649
                "\uD8AF\uE3A3\u0000\u0000\uD7EF\u0000\u0000\u0000\u0000\u0000" + //  8650 -  8659
                "\u0000\uA9F9\u0000\u0000\u0000\uCBAD\u0000\uF9B0\u0000\u0000" + //  8660 -  8669
                "\u0000\uE9E3\u0000\uEDCB\uCFE4\u0000\uF0AB\u0000\u0000\u0000" + //  8670 -  8679
                "\u0000\u0000\uEBE7\u0000\uA2BC\uA2BD\u0000\uA2C0\uA2BB\uA2BE" + //  8680 -  8689
                "\u0000\uA2BF\uBCC0\uBCC1\u0000\uBCC2\uBCC3\uBCC4\u0000\u0000" + //  8690 -  8699
                "\uF8EA\u0000\u0000\u0000\u0000\u0000\u0000\uC8BC\u0000\uC8BD" + //  8700 -  8709
                "\uBCBF\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF2EF\u0000" + //  8710 -  8719
                "\uCAD1\u0000\u0000\u0000\uEAF1\u0000\uD0A6\u0000\uA1CF\u0000" + //  8720 -  8729
                "\uA1CE\u0000\u0000\u0000\u0000\u0000\uFCEE\u0000\u0000\u0000" + //  8730 -  8739
                "\uFCF4\u0000\u0000\u0000\u0000\uC3D6\u0000\u0000\u0000\uDEB1" + //  8740 -  8749
                "\u0000\u0000\u0000\u0000\uC3DF\uC3E0\u0000\u0000\uDCC2\u0000" + //  8750 -  8759
                "\u0000\uF0A7\u0000\u0000\uE6C0\uBCBC\uBCBD\u0000\u0000\uBCBE" + //  8760 -  8769
                "\u0000\u0000\u0000\uF2B7\u0000\u0000\uFAF6\uF6AA\uFAF7\uBCB9" + //  8770 -  8779
                "\uBCBA\u0000\u0000\u0000\u0000\uBCBB\u0000\uF5CF\uE5F3\uF0C2" + //  8780 -  8789
                "\u0000\u0000\u0000\u0000\u0000\uDDBD\u0000\u0000\u0000\uD8C1" + //  8790 -  8799
                "\u0000\u0000\u0000\u0000\uF1FD\u0000\uDEBF\uFBBA\uF9B9\uBCB1" + //  8800 -  8809
                "\u0000\u0000\uBCB2\uBCB3\u0000\uBCB4\uBCB5\uBCAB\u0000\u0000" + //  8810 -  8819
                "\u0000\u0000\uBCAC\u0000\u0000\uCEDA\uFBEB\uDBA6\uDBDE\uD8E5" + //  8820 -  8829
                "\u0000\u0000\uD9A1\u0000\uD8C0\uDCDB\u0000\u0000\uEDBD\uBCAA" + //  8830 -  8839
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD0C8\u0000\uD5D3" + //  8840 -  8849
                "\uF3F5\uF7AE\u0000\u0000\uEFC8\u0000\uCDF3\uBCA8\u0000\u0000" + //  8850 -  8859
                "\u0000\uBCA9\u0000\u0000\u0000\uDBCF\uCBA4\u0000\u0000\uF8E0" + //  8860 -  8869
                "\u0000\uCFDE\u0000\u0000\uCED0\u0000\u0000\u0000\u0000\uC2D6" + //  8870 -  8879
                "\u0000\u0000\u0000\uD3D9\u0000\u0000\u0000\u0000\uA8FB\uA8FC" + //  8880 -  8889
                "\uA8FD\uA8FE\u0000\uA2C4\uA2C5\u0000\u0000\u0000\u0000\u0000" + //  8890 -  8899
                "\u0000\uF4AA\uE9AF\u0000\u0078\u0079\u007A\u007B\u007C\u007D" + //  8900 -  8909
                "\u007E\u007F\uBCA2\u0000\u0000\u0000\uBCA3\u0000\u0000\u0000" + //  8910 -  8919
                "\uF8CB\u0000\u0000\u0000\u0000\u0000\uB4A9\uB4AA\u0000\u0000" + //  8920 -  8929
                "\uFCBE\uD5F1\u0000\u0000\u0000\u0000\u0000\uB9FC\uB9FD\u0000" + //  8930 -  8939
                "\uB9FE\uBBF9\uBBFA\u0000\uBBFB\uBBFC\uBBFD\u0000\u0000\uF8BE" + //  8940 -  8949
                "\u0000\u0000\u0000\u0000\u0000\u0000\uC7E2\u0000\u0000\uDBBC" + //  8950 -  8959
                "\u0000\u0000\u0000\u0000\u0000\u0000\uFCBF\u0000\u0000\uE8C0" + //  8960 -  8969
                "\uE8C1\u0000\u0000\u0000\uCFE3\uE9A2\uBBF8\u0000\u0000\u0000" + //  8970 -  8979
                "\u0000\u0000\u0000\u0000\uCECB\u0000\uE2A3\uD3FC\u0000\u0000" + //  8980 -  8989
                "\uEDE6\u0000\u0000\u0000\uCDF7\uF0DE\u0000\u0000\u0000\uD3B1" + //  8990 -  8999
                "\u0000\u0000\u0000\u0000\uA1CA\u0000\u0000\u0000\u0000\uBEA2" + //  9000 -  9009
                "\uBEA3\u0000\u0000\uCFAD\u0000\u0000\u0000\u0000\uE7F9\uF8A8" + //  9010 -  9019
                "\uBBF5\uBBF6\u0000\u0000\uBBF7\u0000\u0000\u0000\uD6FD\u0000" + //  9020 -  9029
                "\u0000\u0000\u0000\u0000\uB3FD\uB3FE\u0000\uB4A1\uBBF2\uBBF3" + //  9030 -  9039
                "\u0000\u0000\u0000\uBBF4\u0000\u0000\uDEBE\u0000\u0000\u0000" + //  9040 -  9049
                "\u0000\u0000\u0000\uC7A8\u0000\uC7A9\uBBEA\u0000\u0000\uBBEB" + //  9050 -  9059
                "\uBBEC\uBBED\uBBEE\u0000\uDED7\u0000\u0000\u0000\u0000\u0000" + //  9060 -  9069
                "\uCBDF\u0000\uA2C2\u0000\u0000\uA1DB\u0000\u0000\uA1DD\uA1DC" + //  9070 -  9079
                "\uBBE3\uBBE4\u0000\uBBE5\u0000\uBBE6\u0000\u0000\uD5AD\u0000" + //  9080 -  9089
                "\u0000\u0000\u0000\u0000\u0000\uC7A2\u0000\u0000\uD9D5\u0000" + //  9090 -  9099
                "\u0000\uDFAA\u0000\u0000\u0000\uD0E7\u0000\u0000\uECFD\u0000" + //  9100 -  9109
                "\uD2AE\uBBE2\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uFCC2" + //  9110 -  9119
                "\uEABB\uBBDF\uBBE0\u0000\u0000\uBBE1\u0000\u0000\u0000\uE3A7" + //  9120 -  9129
                "\u0000\uDFD6\uFDE8\u0000\u0000\uDDBF\u0000\u0000\u0000\uF6EF" + //  9130 -  9139
                "\u0000\uDEF9\uBBDD\uBBDE\u0000\u0000\u0000\u0000\u0000\u0000" + //  9140 -  9149
                "\uD0FB\uECDB\u0000\u0000\uDAE0\u0000\u0000\u0000\u0000\u0000" + //  9150 -  9159
                "\u0000\uC5CC\u0000\u0000\uE7A8\u0000\u0000\u0000\u0000\u0000" + //  9160 -  9169
                "\u0000\uF5E3\u0000\u0000\uD4CD\u0000\u0000\uF3B8\u0000\u0000" + //  9170 -  9179
                "\u0000\uA2A1\u0000\uA2A2\u0000\u0000\u0000\uCFB0\uF7D9\uF3E6" + //  9180 -  9189
                "\u0000\u0000\uCDF5\u0000\u0000\u0000\uFDB0\uD5A8\u0000\uE6C8" + //  9190 -  9199
                "\u0000\u0000\u0000\u0000\uF8DA\u0000\u0000\uD3E4\uF6F7\u0000" + //  9200 -  9209
                "\u0000\uD5BA\uF3CD\uCBE1\uBBDC\u0000\u0000\u0000\u0000\u0000" + //  9210 -  9219
                "\u0000\u0000\uEBA2\uEBA3\uBBDA\u0000\u0000\u0000\uBBDB\u0000" + //  9220 -  9229
                "\u0000\u0000\uECCD\u0000\uECCE\u0000\uD6BF\u0000\uCFAC\uF0F0" + //  9230 -  9239
                "\u0000\uF4FD\uDBC8\u0000\u0000\u0000\uECE2\u0000\u0000\u0000" + //  9240 -  9249
                "\u0000\uA7A4\u0000\u0000\uA2E0\u0000\uA2B8\uA2B7\u0000\u0000" + //  9250 -  9259
                "\u0000\u0000\uA1DF\uA1DE\uBBD3\u0000\u0000\u0000\uBBD4\u0000" + //  9260 -  9269
                "\u0000\u0000\uD6BE\u0000\u0000\u0000\uE2BA\u0000\uCED8\u0000" + //  9270 -  9279
                "\uCBDE\uF4AC\uDAFB\u0000\uF6E9\uE8F3\uBBCF\u0000\u0000\u0000" + //  9280 -  9289
                "\u0000\u0000\u0000\u0000\uF3E9\u0000\uD2B9\uD5C3\u0000\u0000" + //  9290 -  9299
                "\uDAE5\uDAD0\u0000\uD1D9\uBBCB\uBBCC\u0000\u0000\u0000\uBBCD" + //  9300 -  9309
                "\u0000\u0000\uECF1\u0000\u0000\u0000\u0000\u0000\u0000\uC4FA" + //  9310 -  9319
                "\u0000\u0000\uFDC0\u0000\u0000\u0000\u0000\u0000\u0000\uE7A3" + //  9320 -  9329
                "\u0000\u0000\uE8B1\u0000\uFCAE\u0000\u0000\u0000\u0000\uC6EC" + //  9330 -  9339
                "\u0000\u0000\u0000\uA8AB\uA9AB\u0000\u0000\u0000\u0000\uBDEA" + //  9340 -  9349
                "\u0000\u0000\u0000\uEEA9\uF6BC\u0000\u0000\uCCDB\uBBCA\u0000" + //  9350 -  9359
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE4E4\u0000\uEFB9\u0000" + //  9360 -  9369
                "\u0000\uF8D8\u0000\u0000\u0000\u0000\uC2C9\uC2CA\u0000\u0000" + //  9370 -  9379
                "\uF7CB\uDFAE\uE8F5\u0000\u0000\u0000\u0000\uC2E8\u0000\u0000" + //  9380 -  9389
                "\u0000\uDFD7\uDBD0\uDBD1\u0000\u0000\uE8B6\u0000\u0000\uD6CF" + //  9390 -  9399
                "\uF4BA\u0000\uF7C9\uBBC7\uBBC8\u0000\u0000\uBBC9\u0000\u0000" + //  9400 -  9409
                "\u0000\uF1AA\uCED1\u0000\u0000\uF6C7\u0000\uD0FC\u0000\u0000" + //  9410 -  9419
                "\u0000\uF4FC\u0000\u0000\u0000\uE9FE\u0000\u0000\u0000\u0000" + //  9420 -  9429
                "\uC8F4\uC8F5\u0000\u0000\uD8A4\u0000\u0000\u0000\u0000\u0000" + //  9430 -  9439
                "\uF2A7\uBBC2\uBBC3\u0000\uBBC4\uBBC5\uBBC6\u0000\u0000\uF6EE" + //  9440 -  9449
                "\u0000\uF6CC\uE2F8\u0000\u0000\u0000\uF8C9\u0000\u0000\u0000" + //  9450 -  9459
                "\u0000\uE3D2\uBBC0\uBBC1\u0000\u0000\u0000\u0000\u0000\u0000" + //  9460 -  9469
                "\uFCC6\u0000\u0000\u0000\uF3C3\u0000\u0000\u0000\u0000\u0000" + //  9470 -  9479
                "\uB3BF\uB3C0\u0000\uB3C1\uBBBA\u0000\u0000\uBBBB\uBBBC\uBBBD" + //  9480 -  9489
                "\u0000\u0000\uD1C7\uE9AE\u0000\uE8BD\u0000\u0000\uFAC4\uBBB9" + //  9490 -  9499
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD5B3\u0000\uF6F5" + //  9500 -  9509
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE4E8\uBBB5\uBBB6" + //  9510 -  9519
                "\u0000\u0000\uBBB7\u0000\u0000\uBBB8\uBBB4\u0000\u0000\u0000" + //  9520 -  9529
                "\u0000\u0000\u0000\u0000\uD5EB\u0000\uFBC3\uDDEA\u0000\uE2A2" + //  9530 -  9539
                "\u0000\uEEA6\u0000\u0000\uE4C4\u0000\u0000\u0000\u0000\u0000" + //  9540 -  9549
                "\u0000\uF5C5\uEEE0\u0000\uA2C9\uA2CC\u0000\u0000\u0000\u0000" + //  9550 -  9559
                "\u0000\u0000\uE4C3\u0000\u0000\uDBC6\uD0F1\u0000\uD0F2\u0000" + //  9560 -  9569
                "\u0000\u0000\uF6DA\u0000\u0000\u0000\u0000\uC5DB\uC5DC\u0000" + //  9570 -  9579
                "\uC5DD\uBBB2\uBBB3\u0000\u0000\u0000\u0000\u0000\u0000\uE1D5" + //  9580 -  9589
                "\u0000\u0000\uD4EA\uBBB0\uBBB1\u0000\u0000\u0000\u0000\u0000" + //  9590 -  9599
                "\u0000\uE9D6\u0000\u0000\u0000\uD1CC\uDDFC\u0000\u0000\u0000" + //  9600 -  9609
                "\u0000\uC7C4\uC7C5\u0000\uC7C6\uBBAB\u0000\u0000\u0000\uBBAC" + //  9610 -  9619
                "\u0000\u0000\u0000\uF0FB\uFAE1\uF0DA\uCCE7\uDAF4\u0000\uE1B6" + //  9620 -  9629
                "\uF8B7\u0000\u0000\u0000\u0000\u0000\uE0BF\uBBA3\uBBA4\u0000" + //  9630 -  9639
                "\uBBA5\uBBA6\uBBA7\u0000\u0000\uD0CF\u0000\uCFFA\uF3CA\uE0D7" + //  9640 -  9649
                "\u0000\u0000\uEEC9\u0000\u0000\u0000\uE2DD\u0000\u0000\uDFE2" + //  9650 -  9659
                "\uE7DB\u0000\u0000\u0000\u0000\u0000\uB2CF\uB2D0\u0000\u0000" + //  9660 -  9669
                "\uDAC8\uDFA6\u0000\uF9B3\uF2D2\u0000\uCAC4\uBBA1\u0000\uBBA2" + //  9670 -  9679
                "\u0000\u0000\u0000\u0000\u0000\uBCC5\uBCC6\u0000\u0000\uFAC3" + //  9680 -  9689
                "\uE5D7\u0000\uECC8\u0000\u0000\u0000\uCBEF\uFCDF\u0000\u0000" + //  9690 -  9699
                "\u0000\u0000\uC1EA\u0000\u0000\u0000\uE3ED\u0000\uE8C2\u0000" + //  9700 -  9709
                "\uEDF5\uBAFC\uBAFD\u0000\u0000\uBAFE\u0000\u0000\u0000\uFBD5" + //  9710 -  9719
                "\u0000\u0000\u0000\u0000\u0000\uB2EE\u0000\u0000\u0000\uCDFA" + //  9720 -  9729
                "\u0000\u0000\u0000\u0000\u0000\uF9ED\u0000\u0000\u0000\uD8C8" + //  9730 -  9739
                "\u0000\u0000\uEEC1\u0000\uEEFA\uFDF4\u0000\u0000\uD3E3\u0000" + //  9740 -  9749
                "\uFBC2\u0000\uA1E1\uA1E0\u0000\uA2C3\uA2C7\uA2C8\uA2CB\uA2CA" + //  9750 -  9759
                "\uBAF3\u0000\u0000\u0000\uBAF4\u0000\uBAF5\u0000\uE0F7\uE4B2" + //  9760 -  9769
                "\uCCFC\u0000\u0000\u0000\uFBE4\u0000\uA6E2\uA6E3\uA6E4\uA6B6" + //  9770 -  9779
                "\u0000\u0000\u0000\u0000\uBEAD\u0000\u0000\u0000\uD0A7\u0000" + //  9780 -  9789
                "\uF0CB\u0000\uD0C7\uBAEC\u0000\u0000\u0000\uBAED\u0000\u0000" + //  9790 -  9799
                "\u0000\uD8A3\u0000\u0000\uDAD9\u0000\uF0D8\uBAE7\u0000\u0000" + //  9800 -  9809
                "\uBAE8\u0000\uBAE9\u0000\u0000\uE1E2\uD1C6\u0000\u0000\u0000" + //  9810 -  9819
                "\u0000\u0000\uB9F6\uB9F7\u0000\u0000\uDCCC\u0000\uD0CB\u0000" + //  9820 -  9829
                "\u0000\u0000\uFCA4\uBAE6\u0000\u0000\u0000\u0000\u0000\u0000" + //  9830 -  9839
                "\u0000\uFCE8\uF3BD\uBAE4\u0000\u0000\u0000\uBAE5\u0000\u0000" + //  9840 -  9849
                "\u0000\uEDD8\uE1C7\u0000\u0000\u0000\u0000\uC6F7\uC6F8\u0000" + //  9850 -  9859
                "\u0000\uF7C7\u0000\u0000\u0000\u0000\u0000\u0000\uC2C6\u0000" + //  9860 -  9869
                "\u0000\uD6E1\uCFD2\u0000\uD0B6\u0000\u0000\u0000\uE9D0\u0000" + //  9870 -  9879
                "\u0000\u0000\u0000\u0000\uFABC\uE6E2\u0000\u0000\uF6C4\u0000" + //  9880 -  9889
                "\u0000\u0000\uEEDD\uE7C4\u0000\uCDFC\u0000\uD9E6\u0000\uE2F9" + //  9890 -  9899
                "\uE2A1\uEBD4\u0000\uA6DB\uA6DC\uA6C0\uA6DD\uA6DE\uA6DF\uA6E0" + //  9900 -  9909
                "\uA6E1\uBAE1\u0000\u0000\u0000\uBAE2\u0000\u0000\u0000\uE3D9" + //  9910 -  9919
                "\u0000\u0000\u0000\u0000\u0000\uB2E3\u0000\u0000\u0000\uA1EC" + //  9920 -  9929
                "\uA1ED\u0000\u0000\u0000\u0000\uBEA5\uBEA6\u0000\u0000\uFAD4" + //  9930 -  9939
                "\u0000\u0000\u0000\uECE5\u0000\u0000\uD7E9\uEDF6\u0000\u0000" + //  9940 -  9949
                "\u0000\uDEED\u0000\uD5FC\u0000\u0000\u0000\u0000\u0000\u0000" + //  9950 -  9959
                "\uD3D4\uBADE\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uFBC9" + //  9960 -  9969
                "\u0000\uFAD8\u0000\uF3D5\u0000\uCFAB\u0000\u0000\uEBF3\uBADD" + //  9970 -  9979
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uDBF1\u0000\uFAAB" + //  9980 -  9989
                "\uEBEB\uE7F8\uD9E5\u0000\u0000\u0000\u0000\uC2C0\uC2C1\u0000" + //  9990 -  9999
                "\uC2C2\uBAD5\uBAD6\u0000\uBAD7\u0000\uBAD8\u0000\u0000\uF4EC" + // 10000 - 10009
                "\u0000\u0000\u0000\u0000\uEFFE\u0000\uFDF3\uFDF2\uF7A6\u0000" + // 10010 - 10019
                "\u0000\u0000\u0000\u0000\uF9EF\uCFF4\uF7E6\u0000\uA6BF\uA6D7" + // 10020 - 10029
                "\uA6D8\uA6B5\uA6AB\uA6D9\uA6DA\uA6BB\uBAD2\uBAD3\uBAD4\u0000" + // 10030 - 10039
                "\u0000\u0000\u0000\u0000\uBCB6\uBCB7\u0000\uBCB8\uBACE\uBACF" + // 10040 - 10049
                "\u0000\u0000\uBAD0\u0000\u0000\uBAD1\uBACD\u0000\u0000\u0000" + // 10050 - 10059
                "\u0000\u0000\u0000\u0000\uD6B1\uDEB2\uBACA\uBACB\u0000\u0000" + // 10060 - 10069
                "\u0000\u0000\u0000\u0000\uE0F4\u0000\uCFE0\u0000\uD5FB\uDEBB" + // 10070 - 10079
                "\u0000\u0000\uF4FB\u0000\u0000\u0000\uFBD9\uEDF7\u0000\u0000" + // 10080 - 10089
                "\uE5B5\uBAC9\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE3D6" + // 10090 - 10099
                "\u0000\uEAD0\u0000\u0000\u0000\u0000\u0000\uCCD4\uCBAF\uBAC6" + // 10100 - 10109
                "\uBAC7\u0000\u0000\uBAC8\u0000\u0000\u0000\uE2B5\u0000\u0000" + // 10110 - 10119
                "\uCAE6\u0000\uD3AE\uBAC5\u0000\u0000\u0000\u0000\u0000\u0000" + // 10120 - 10129
                "\u0000\uFDE4\uFACE\uBAC1\u0000\u0000\u0000\uBAC2\u0000\u0000" + // 10130 - 10139
                "\u0000\uEFBC\uD8A1\u0000\u0000\u0000\u0000\uC6EF\uC6F0\u0000" + // 10140 - 10149
                "\u0000\uDCA4\u0000\u0000\u0000\uF0B8\u0000\u0000\uECBF\uFCD8" + // 10150 - 10159
                "\u0000\u0000\u0000\u0000\u0000\uB4BA\uB4BB\u0000\u0000\uF2F1" + // 10160 - 10169
                "\u0000\u0000\u0000\u0000\u0000\u0000\uEFF7\u0000\u0000\uE8AC" + // 10170 - 10179
                "\u0000\uE8DD\u0000\u0000\uEFE9\u0000\uCBC6\u0000\u0000\uF0F6" + // 10180 - 10189
                "\u0000\u0000\uD5E7\u0000\uA6BD\uA6D3\uA6D4\uA6B3\uA6AA\uA6D5" + // 10190 - 10199
                "\uA6D6\uA6BA\uBABB\u0000\u0000\u0000\uBABC\u0000\u0000\u0000" + // 10200 - 10209
                "\uF2F2\uF3EB\u0000\uF0D7\u0000\u0000\uE7CE\u0000\u0000\uDFDC" + // 10210 - 10219
                "\u0000\uF9C7\u0000\uF3D3\uF3D4\u0000\u0000\u0000\uF7E4\u0000" + // 10220 - 10229
                "\uF7D1\uBAB6\u0000\u0000\u0000\uBAB7\u0000\u0000\u0000\uE0C9" + // 10230 - 10239
                "\u0000\u0000\u0000\uD6C9\u0000\uEDB1\u0000\uCCC3\uF7BE\uFCCB" + // 10240 - 10249
                "\u0000\u0000\u0000\uCEA2\u0000\u0000\uF3EE\u0000\uA6B9\uA6CF" + // 10250 - 10259
                "\uA6D0\uA6B4\uA6A8\uA6D1\uA6D2\uA6B8\uBAB3\uBAB4\u0000\u0000" + // 10260 - 10269
                "\u0000\uBAB5\u0000\u0000\uEFC5\u0000\uE7E7\u0000\u0000\uD7FD" + // 10270 - 10279
                "\u0000\uFCCA\u0000\u0000\u0000\uF3E1\u0000\u0000\uCBC4\uBAAF" + // 10280 - 10289
                "\u0000\u0000\u0000\uBAB0\u0000\u0000\u0000\uD1B8\u0000\u0000" + // 10290 - 10299
                "\u0000\u0000\uD6DF\uBAA8\uBAA9\u0000\uBAAA\uBAAB\uBAAC\u0000" + // 10300 - 10309
                "\u0000\uDADF\u0000\uEFB3\u0000\u0000\u0000\u0000\uFAA7\u0000" + // 10310 - 10319
                "\u0000\u0000\u0000\uC0EB\uC0EC\u0000\uC0ED\uBAA7\u0000\u0000" + // 10320 - 10329
                "\u0000\u0000\u0000\u0000\u0000\uFACB\u0000\uEAEF\uEAF0\u0000" + // 10330 - 10339
                "\u0000\u0000\uDAC0\uF8B4\uEBF2\uBAA3\uBAA4\u0000\u0000\uBAA5" + // 10340 - 10349
                "\u0000\u0000\uBAA6\uB9F8\u0000\u0000\uB9F9\uB9FA\u0000\uB9FB" + // 10350 - 10359
                "\u0000\uF6E7\uD2DD\u0000\uDFCC\u0000\u0000\uFCC9\u0000\uA6B7" + // 10360 - 10369
                "\uA6CB\uA6CC\uA6B2\uA6A9\uA6BE\uA6CD\uA6CE\uB9F4\u0000\u0000" + // 10370 - 10379
                "\u0000\u0000\u0000\u0000\u0000\uE7B4\u0000\uFBB3\uE4C2\u0000" + // 10380 - 10389
                "\u0000\u0000\u0000\u0000\u0000\uEFA9\u0000\u0000\uD3FB\u0000" + // 10390 - 10399
                "\u0000\u0000\u0000\u0000\u0000\uE8B2\u0000\u0000\uF3B5\u0000" + // 10400 - 10409
                "\u0000\uF8A4\u0000\u0000\uD1F3\uB9EC\uB9ED\u0000\uB9EE\uB9EF" + // 10410 - 10419
                "\uB9F0\u0000\u0000\uF6CB\u0000\uF1E6\uEDC1\uE8BC\uEED1\u0000" + // 10420 - 10429
                "\uCCFB\u0000\u0000\u0000\uD3FA\uF4A4\u0000\u0000\uE4CE\u0000" + // 10430 - 10439
                "\u0000\u0000\u0000\u0000\u0000\uF1D4\u0000\uEDF2\uB9EB\u0000" + // 10440 - 10449
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF7D2\u0000\uF0F5\u0000" + // 10450 - 10459
                "\uDDE8\uD3ED\uF5FC\u0000\uDABF\u0000\uA6A5\uA6C4\uA6C3\uA6B0" + // 10460 - 10469
                "\uA6A7\uA6BC\uA6C9\uA6CA\uB9E8\uB9E9\u0000\u0000\uB9EA\u0000" + // 10470 - 10479
                "\u0000\u0000\uF0D4\uCBE4\uFBD4\uF5E6\uE3EA\u0000\uEAED\u0000" + // 10480 - 10489
                "\u0000\uFCB4\uF5C2\u0000\u0000\uD7DC\uB9DD\u0000\u0000\uB9DE" + // 10490 - 10499
                "\uB9DF\uB9E0\uB9E1\uB9E2\uB9D2\uB9D3\u0000\uB9D4\uB9D5\uB9D6" + // 10500 - 10509
                "\u0000\uB9D7\uB9D0\u0000\uB9D1\u0000\u0000\u0000\u0000\u0000" + // 10510 - 10519
                "\uBCA4\uBCA5\u0000\uBCA6\uB9CC\uB9CD\u0000\u0000\uB9CE\u0000" + // 10520 - 10529
                "\u0000\uB9CF\uB9CA\u0000\u0000\uB9CB\u0000\u0000\u0000\u0000" + // 10530 - 10539
                "\uF6E8\u0000\u0000\u0000\u0000\uC2C7\u0000\u0000\u0000\uE3C6" + // 10540 - 10549
                "\u0000\u0000\u0000\uDEE4\u0000\uFDE6\u0000\u0000\u0000\u0000" + // 10550 - 10559
                "\u0000\u0000\u0000\uF6BA\uB9C9\u0000\u0000\u0000\u0000\u0000" + // 10560 - 10569
                "\u0000\u0000\uCAD2\u0000\uF9CF\uEBDA\uCAC1\u0000\uD2B8\uCDF1" + // 10570 - 10579
                "\u0000\uE3D3\uB9C7\u0000\u0000\u0000\uB9C8\u0000\u0000\u0000" + // 10580 - 10589
                "\uD5EC\uD5F8\uDAF3\u0000\u0000\u0000\uC8B6\u0000\uC8B7\u0000" + // 10590 - 10599
                "\u0000\uF4C6\u0000\u0000\u0000\u0000\u0000\u0000\uC1D5\u0000" + // 10600 - 10609
                "\u0000\uE2BC\u0000\u0000\uFCED\uECE0\uD2FE\u0000\uCDF0\u0000" + // 10610 - 10619
                "\uF9F6\u0000\u0000\uDFF0\u0000\u0000\uF2A5\u0000\u0000\u0000" + // 10620 - 10629
                "\u0000\u0000\uF2A6\uB9C3\u0000\u0000\u0000\uB9C4\u0000\u0000" + // 10630 - 10639
                "\u0000\uD6F0\uF3AF\u0000\u0000\uCDA5\u0000\uEAB3\u0000\uCED6" + // 10640 - 10649
                "\u0000\u0000\u0000\u0000\uCCA5\uB9C1\u0000\u0000\u0000\u0000" + // 10650 - 10659
                "\u0000\u0000\u0000\uD9D4\uD6E8\uB9BF\u0000\u0000\u0000\uB9C0" + // 10660 - 10669
                "\u0000\u0000\u0000\uFCAC\uFCAD\uD8A7\u0000\u0000\u0000\uC1C1" + // 10670 - 10679
                "\uC1C2\uC1C3\u0000\u0000\uF4C5\uDCA3\u0000\u0000\u0000\u0000" + // 10680 - 10689
                "\u0000\uB8FE\u0000\u0000\u0000\uCBD4\uE0BE\uE3F8\uEAE9\uFCB2" + // 10690 - 10699
                "\u0000\uF7BD\uECAE\u0000\u0000\u0000\uD0E1\u0000\uE0F5\uB9BB" + // 10700 - 10709
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE4F2\u0000\uF5F7" + // 10710 - 10719
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCFA9\uB9B9\u0000" + // 10720 - 10729
                "\u0000\u0000\uB9BA\u0000\u0000\u0000\uF8DF\u0000\uF7F2\u0000" + // 10730 - 10739
                "\u0000\u0000\uC0D2\uC0D3\uC0D4\u0000\uC0D5\uB9AE\u0000\u0000" + // 10740 - 10749
                "\uB9AF\uB9B0\uB9B1\uB9B2\u0000\uD8CE\uD8CF\u0000\u0000\u0000" + // 10750 - 10759
                "\u0000\u0000\u0000\uE6DD\u0000\u0000\uF8D9\u0000\u0000\u0000" + // 10760 - 10769
                "\uEEBD\u0000\u0000\uD8C6\u0000\u0000\uE4E3\uF5CE\u0000\u0000" + // 10770 - 10779
                "\uCDF2\u0000\uCFEB\u0000\u0000\u0000\uCDB8\uB9A8\u0000\u0000" + // 10780 - 10789
                "\u0000\u0000\u0000\u0000\u0000\uDCB5\uE0F8\uB9A6\u0000\u0000" + // 10790 - 10799
                "\u0000\uB9A7\u0000\u0000\u0000\uDDF7\u0000\u0000\u0000\u0000" + // 10800 - 10809
                "\u0000\uB2CD\uB2CE\u0000\u0000\uF9D8\uF9D9\uCAE0\uDACA\u0000" + // 10810 - 10819
                "\u0000\u0000\uDAAC\uEAB0\u0000\u0000\u0000\u0000\uC1C9\uC1CA" + // 10820 - 10829
                "\u0000\u0000\uF1E8\uD9F2\uDBF5\uCAB5\uD9C6\u0000\u0000\uDCBC" + // 10830 - 10839
                "\uD2B6\uF5D5\u0000\u0000\u0000\u0000\uC6DB\uC6DC\u0000\u0000" + // 10840 - 10849
                "\uE2AD\u0000\u0000\u0000\u0000\u0000\u0000\uE3DA\u0000\uFCD9" + // 10850 - 10859
                "\uB9A1\u0000\u0000\u0000\uB9A2\u0000\u0000\u0000\uE3C5\uEBF8" + // 10860 - 10869
                "\u0000\uF2B1\u0000\u0000\uF8CD\u0000\uCBD2\u0000\u0000\u0000" + // 10870 - 10879
                "\uEBCE\uB8FC\uB8FD\u0000\u0000\u0000\u0000\u0000\u0000\uE5F2" + // 10880 - 10889
                "\u0000\u0000\uD0F4\uB8FB\u0000\u0000\u0000\u0000\u0000\u0000" + // 10890 - 10899
                "\u0000\uCDE0\uE3B0\uB8F6\uB8F7\u0000\uB8F8\u0000\uB8F9\u0000" + // 10900 - 10909
                "\u0000\uCAFC\uCAFD\u0000\u0000\u0000\u0000\u0000\uB8EF\u0000" + // 10910 - 10919
                "\u0000\u0000\uD8DA\u0000\u0000\u0000\u0000\u0000\uD6E2\u0000" + // 10920 - 10929
                "\u0000\u0000\uEFA8\u0000\u0000\u0000\uEEB4\uB8F4\u0000\uB8F5" + // 10930 - 10939
                "\u0000\u0000\u0000\u0000\u0000\uBBFE\uBCA1\u0000\u0000\uCCF0" + // 10940 - 10949
                "\u0000\u0000\uD7AF\u0000\u0000\u0000\uDDE6\u0000\u0000\u0000" + // 10950 - 10959
                "\uDCFE\u0000\uFCC7\uDCD6\uE2E0\u0000\u0000\u0000\uDAB0\u0000" + // 10960 - 10969
                "\uA6A4\uA6C2\uA6C1\uA6AF\uA6A6\uA6C6\uA6C5\uA6B1\uB8F0\uB8F1" + // 10970 - 10979
                "\u0000\uB8F2\uB8F3\u0000\u0000\u0000\uD8EE\u0000\uF2C1\u0000" + // 10980 - 10989
                "\u0000\u0000\uBBA8\uBBA9\uBBAA\u0000\u0000\uEEEB\u0000\u0000" + // 10990 - 10999
                "\u0000\u0000\u0000\u0000\uBDE0\u0000\u0000\uE7C8\u0000\u0000" + // 11000 - 11009
                "\u0000\u0000\u0000\u0000\uFAB4\u0000\u0000\uE9E0\u0000\u0000" + // 11010 - 11019
                "\u0000\uD0D8\uFCA2\uD4BE\uB8EA\u0000\u0000\u0000\u0000\u0000" + // 11020 - 11029
                "\u0000\u0000\uF3E2\u0000\uD5D0\uE5D9\u0000\u0000\u0000\u0000" + // 11030 - 11039
                "\u0000\u0000\uFCA1\uEFEE\uDCD8\uB8E7\uB8E8\u0000\u0000\uB8E9" + // 11040 - 11049
                "\u0000\u0000\u0000\uF9EC\u0000\u0000\u0000\uCBCC\u0000\uDBC7" + // 11050 - 11059
                "\uDED5\u0000\u0000\u0000\u0000\uF0F4\u0000\uA6A1\uA6AC\uA6A2" + // 11060 - 11069
                "\uA6AD\u0000\u0000\u0000\u0000\uBEA9\uBEAA\u0000\u0000\uF7E7" + // 11070 - 11079
                "\u0000\u0000\uCDDE\u0000\uF7A4\u0000\u0070\u0071\u0072\u0073" + // 11080 - 11089
                "\u0074\u0075\u0076\u0077\uB8E5\uB8E6\u0000\u0000\u0000\u0000" + // 11090 - 11099
                "\u0000\u0000\uD0F3\uE0AA\uE8E2\u0000\uDFEF\uCCD3\uD3F9\u0000" + // 11100 - 11109
                "\u0000\u0000\u0000\uD4F0\uB8E0\u0000\u0000\u0000\uB8E1\u0000" + // 11110 - 11119
                "\u0000\u0000\uE9DD\uDBCD\u0000\u0000\uDDCE\u0000\uE8D9\uEFD6" + // 11120 - 11129
                "\u0000\u0000\u0000\uD3E2\u0000\uE2DF\uB8D8\uB8D9\u0000\uB8DA" + // 11130 - 11139
                "\u0000\uB8DB\uB8DC\u0000\uF3E0\uE7AF\u0000\u0000\u0000\u0000" + // 11140 - 11149
                "\u0000\uDBAD\uB8D6\u0000\uB8D7\u0000\u0000\u0000\u0000\u0000" + // 11150 - 11159
                "\uBBEF\uBBF0\u0000\uBBF1\uB8D3\uB8D4\u0000\u0000\uB8D5\u0000" + // 11160 - 11169
                "\u0000\u0000\uF5D4\u0000\u0000\u0000\u0000\uD9A9\uB8D1\u0000" + // 11170 - 11179
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE2A5\uCDB9\uB8CF\uB8D0" + // 11180 - 11189
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE5BD\u0000\u0000\u0000" + // 11190 - 11199
                "\uEBA8\u0000\u0000\u0000\uDBFE\u0000\uF7A5\u0000\uCBAE\u0000" + // 11200 - 11209
                "\uDAAF\u0000\uD8B6\u0000\uA8E5\uA8E6\u0000\u0000\u0000\u0000" + // 11210 - 11219
                "\u0000\u0000\uEFD7\u0000\uD4C3\uB8CC\uB8CD\uB8CE\u0000\u0000" + // 11220 - 11229
                "\u0000\u0000\u0000\uBBE7\uBBE8\u0000\uBBE9\uB8C7\u0000\u0000" + // 11230 - 11239
                "\u0000\uB8C8\u0000\u0000\u0000\uD6FB\u0000\u0000\u0000\u0000" + // 11240 - 11249
                "\u0000\uB2AD\uB2AE\u0000\uB2AF\uB8BE\uB8BF\u0000\uB8C0\u0000" + // 11250 - 11259
                "\uB8C1\uB8C2\u0000\uFBF0\u0000\u0000\uECAC\u0000\u0000\u0000" + // 11260 - 11269
                "\uF0A9\uB8BB\uB8BC\uB8BD\u0000\u0000\u0000\u0000\u0000\uBBD8" + // 11270 - 11279
                "\u0000\u0000\u0000\uDDF6\u0000\uCDC0\u0000\u0000\u0000\uB4E2" + // 11280 - 11289
                "\uB4E3\uB4E4\u0000\uB4E5\uB8B6\uB8B7\u0000\u0000\uB8B8\u0000" + // 11290 - 11299
                "\uB8B9\uB8BA\uB8B0\u0000\u0000\u0000\uB8B1\u0000\u0000\u0000" + // 11300 - 11309
                "\uD7E5\u0000\u0000\u0000\u0000\u0000\uB2A8\uB2A9\uB2AA\u0000" + // 11310 - 11319
                "\uF3DF\u0000\uF8C8\uCEC6\u0000\u0000\u0000\u0000\uC1E2\u0000" + // 11320 - 11329
                "\u0000\u0000\uF0FE\u0000\u0000\u0000\u0000\uC7FD\u0000\u0000" + // 11330 - 11339
                "\u0000\uE0CE\u0000\uF5FD\u0000\u0000\uE5E4\uDFF1\u0000\u0000" + // 11340 - 11349
                "\uF7E1\u0000\uF9F7\uB8A5\u0000\u0000\u0000\uB8A6\u0000\u0000" + // 11350 - 11359
                "\u0000\uE5CA\u0000\uF6C0\uFDDD\u0000\u0000\uE0D5\u0000\uEFB0" + // 11360 - 11369
                "\u0000\u0000\uE2C7\u0000\uCFAA\u0000\u0000\uCEA9\u0000\u0000" + // 11370 - 11379
                "\uD6F8\u0000\uA8DD\uA8DE\uA8DF\uA8E0\uA8E1\uA8E2\uA8E3\uA8E4" + // 11380 - 11389
                "\uB7FD\uB7FE\u0000\uB8A1\u0000\uB8A2\u0000\u0000\uFAE8\u0000" + // 11390 - 11399
                "\u0000\u0000\u0000\u0000\u0000\uBCD1\u0000\u0000\uD0D9\u0000" + // 11400 - 11409
                "\u0000\uDDD2\uF7F4\uE7DC\uE4A5\uB7FC\u0000\u0000\u0000\u0000" + // 11410 - 11419
                "\u0000\u0000\u0000\uE3A6\uD1DA\uB7F9\uB7FA\u0000\u0000\uB7FB" + // 11420 - 11429
                "\u0000\u0000\u0000\uCAB2\u0000\u0000\uDCBB\u0000\uF1F8\uB7F4" + // 11430 - 11439
                "\u0000\u0000\u0000\uB7F5\u0000\u0000\u0000\uDBE3\u0000\u0000" + // 11440 - 11449
                "\u0000\u0000\uF1E1\uB7F1\u0000\u0000\u0000\u0000\u0000\u0000" + // 11450 - 11459
                "\u0000\uDDCA\u0000\uF9BF\uD6AF\uD5C6\u0000\u0000\u0000\u0000" + // 11460 - 11469
                "\u0000\uFCF5\u0000\u0000\u0000\uE4FD\u0000\u0000\uE3EC\u0000" + // 11470 - 11479
                "\uA8D5\uA8D6\uA8D7\uA8D8\uA8D9\uA8DA\uA8DB\uA8DC\uB7F0\u0000" + // 11480 - 11489
                "\u0000\u0000\u0000\u0000\u0000\u0000\uD5E8\uDBAE\uB7EB\uB7EC" + // 11490 - 11499
                "\u0000\uB7ED\u0000\uB7EE\u0000\u0000\uEBB8\u0000\uE0B7\uD7EC" + // 11500 - 11509
                "\uF1EC\uE5AF\uD5E1\uB7EA\u0000\u0000\u0000\u0000\u0000\u0000" + // 11510 - 11519
                "\u0000\uD1D7\u0000\uE9BC\u0000\u0000\u0000\u0000\u0000\uEAEC" + // 11520 - 11529
                "\u0000\uA8CD\uA8CE\uA8CF\uA8D0\uA8D1\uA8D2\uA8D3\uA8D4\uB7E7" + // 11530 - 11539
                "\uB7E8\u0000\u0000\uB7E9\u0000\u0000\u0000\uF9B4\u0000\u0000" + // 11540 - 11549
                "\uD5D4\uFDCF\u0000\uDFCA\u0000\u0000\u0000\u0000\u0000\uD3F8" + // 11550 - 11559
                "\uF1A8\uB7E2\u0000\u0000\u0000\uB7E3\u0000\u0000\u0000\uD1AF" + // 11560 - 11569
                "\uD7E3\u0000\u0000\u0000\uE0C6\uB7DD\uB7DE\u0000\uB7DF\u0000" + // 11570 - 11579
                "\uB7E0\u0000\u0000\uDBA3\u0000\u0000\uD6CA\uCBD9\u0000\u0000" + // 11580 - 11589
                "\uF3ED\u0000\u0000\u0000\u0000\u0000\u0000\uDCE0\u0000\u0000" + // 11590 - 11599
                "\uD4AA\u0000\uE5CC\u0000\u0000\u0000\u0000\uC6D9\uC6DA\u0000" + // 11600 - 11609
                "\u0000\uEEC3\u0000\u0000\u0000\u0000\u0000\u0000\uD5B5\u0000" + // 11610 - 11619
                "\u0000\uFCB9\uEEC2\uCAF5\u0000\u0000\u0000\uEFE5\uB7DC\u0000" + // 11620 - 11629
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF5BC\uF2A4\uB7DA\u0000" + // 11630 - 11639
                "\u0000\u0000\uB7DB\u0000\u0000\u0000\uD6BC\uD3E5\u0000\u0000" + // 11640 - 11649
                "\u0000\u0000\uC5C6\uC5C7\u0000\uC5C8\uB7D9\u0000\u0000\u0000" + // 11650 - 11659
                "\u0000\u0000\u0000\u0000\uD3A7\uFBB2\uB7D6\u0000\u0000\u0000" + // 11660 - 11669
                "\uB7D7\u0000\u0000\u0000\uE7D9\u0000\u0000\u0000\u0000\u0000" + // 11670 - 11679
                "\uB1D4\u0000\u0000\u0000\uDCF3\uD9B0\u0000\uE6E9\u0000\uFAB9" + // 11680 - 11689
                "\uCACF\u0000\uFCB3\uEAEA\uEAEB\uD0FA\u0000\uA9E1\uA9E2\uA9E3" + // 11690 - 11699
                "\uA9E4\uA9E5\uA9E6\u0000\u0000\uD6B7\uCDB3\u0000\u0000\u0000" + // 11700 - 11709
                "\u0000\u0000\uEEE5\u0000\uDEF5\u0000\u0068\u0069\u006A\u006B" + // 11710 - 11719
                "\u006C\u006D\u006E\u006F\uB7D0\u0000\u0000\u0000\uB7D1\u0000" + // 11720 - 11729
                "\u0000\u0000\uDFE5\u0000\u0000\u0000\u0000\u0000\uB1CB\u0000" + // 11730 - 11739
                "\u0000\u0000\uCBF6\u0000\u0000\u0000\u0000\uD2B3\uD2BF\u0000" + // 11740 - 11749
                "\u0000\u0000\uE2D0\uF4F7\u0000\u0000\u0000\uCEF7\u0000\u0000" + // 11750 - 11759
                "\uE0D8\u0000\uEFD3\u0000\u0000\u0000\uE4C1\uF8EB\u0000\uDBAC" + // 11760 - 11769
                "\uB7CA\u0000\u0000\u0000\uB7CB\u0000\u0000\u0000\uF4E0\u0000" + // 11770 - 11779
                "\u0000\u0000\u0000\u0000\uB1C9\uB1CA\u0000\u0000\uF6AD\u0000" + // 11780 - 11789
                "\uF5B3\u0000\uF0B5\u0000\u0000\uE0CF\u0000\u0000\u0000\u0000" + // 11790 - 11799
                "\u0000\u0000\uD8C3\u0000\u0000\uE9DF\u0000\u0000\u0000\u0000" + // 11800 - 11809
                "\u0000\u0000\uE9E8\u0000\u0000\uF7E5\u0000\u0000\u0000\uCCB2" + // 11810 - 11819
                "\u0000\u0000\uD3BF\u0000\u0000\u0000\u0000\u0000\u0000\uF0B0" + // 11820 - 11829
                "\u0000\u0000\uE3FE\uD1AA\uE8AA\u0000\uEAB6\uF9FA\uE6CC\uB7C8" + // 11830 - 11839
                "\uB7C9\u0000\u0000\u0000\u0000\u0000\u0000\uDEAD\u0000\uFABF" + // 11840 - 11849
                "\u0000\uDFC8\u0000\u0000\u0000\u0000\uD9B6\u0000\uFDAC\uB7C3" + // 11850 - 11859
                "\u0000\u0000\u0000\uB7C4\u0000\u0000\u0000\uE5AC\uFDA1\u0000" + // 11860 - 11869
                "\uDFD0\uECB3\u0000\uCBDD\u0000\u0000\uD9E3\u0000\u0000\uF3AC" + // 11870 - 11879
                "\u0000\uA9D9\uA9DA\uA9DB\uA9DC\uA9DD\uA9DE\uA9DF\uA9E0\uB7BD" + // 11880 - 11889
                "\uB7BE\u0000\uB7BF\u0000\uB7C0\u0000\u0000\uD9AE\uD5AC\u0000" + // 11890 - 11899
                "\uE2C6\u0000\u0000\u0000\uE5FD\uDDE5\uD8CD\u0000\u0000\u0000" + // 11900 - 11909
                "\uD9C3\uD0E8\u0000\u0000\u0000\uE0B4\u0000\u0000\u0000\u0000" + // 11910 - 11919
                "\uC6C4\uC6C5\uC6C6\u0000\uE7AE\u0000\uD6BA\u0000\uDFEC\uE4C0" + // 11920 - 11929
                "\u0000\u0000\uCCBC\uF7EA\u0000\u0000\u0000\u0000\u0000\uCEBC" + // 11930 - 11939
                "\u0000\u0000\u0000\uE2F2\u0000\u0000\u0000\u0000\uA1D8\u0000" + // 11940 - 11949
                "\u0000\u0000\u0000\uBDFC\u0000\u0000\u0000\uE0E0\u0000\u0000" + // 11950 - 11959
                "\u0000\u0000\uA1C9\u0000\u0000\u0000\u0000\uBDFD\u0000\u0000" + // 11960 - 11969
                "\u0000\uFAC8\uD6D7\u0000\uE9E5\uFBDC\uB7BC\u0000\u0000\u0000" + // 11970 - 11979
                "\u0000\u0000\u0000\u0000\uFAD7\uFBC1\uB7B9\uB7BA\u0000\u0000" + // 11980 - 11989
                "\uB7BB\u0000\u0000\u0000\uF0D0\u0000\uF7F0\uEEB3\u0000\u0000" + // 11990 - 11999
                "\uEBB7\uEFF8\uF5DC\uEDCC\uDBD5\uF1CF\u0000\uCEE4\u0000\uE8F2" + // 12000 - 12009
                "\u0000\u0000\u0000\u0000\u0000\uCCC7\u0000\u0000\u0000\uD3C4" + // 12010 - 12019
                "\u0000\u0000\uD6C0\u0000\uA9D1\uA9D2\uA9D3\uA9D4\uA9D5\uA9D6" + // 12020 - 12029
                "\uA9D7\uA9D8\uB7B6\uB7B7\u0000\u0000\u0000\u0000\u0000\uB7B8" + // 12030 - 12039
                "\uB7B1\u0000\u0000\u0000\uB7B2\u0000\u0000\u0000\uDCEC\u0000" + // 12040 - 12049
                "\u0000\u0000\u0000\u0000\uB1B8\uB1B9\u0000\u0000\uDEA9\u0000" + // 12050 - 12059
                "\u0000\u0000\u0000\u0000\u0000\uBBD0\u0000\u0000\uCBB8\u0000" + // 12060 - 12069
                "\u0000\u0000\u0000\u0000\u0000\uF6D0\uEBE6\uDAF9\uB7AC\u0000" + // 12070 - 12079
                "\u0000\u0000\u0000\u0000\u0000\u0000\uD8AC\uF3CC\uB7A5\uB7A6" + // 12080 - 12089
                "\u0000\uB7A7\uB7A8\uB7A9\u0000\u0000\uCFDB\u0000\u0000\u0000" + // 12090 - 12099
                "\u0000\u0000\u0000\uBAB1\u0000\uBAB2\uB7A4\u0000\u0000\u0000" + // 12100 - 12109
                "\u0000\u0000\u0000\u0000\uF8C6\u0000\uD5B9\u0000\u0000\u0000" + // 12110 - 12119
                "\u0000\u0000\u0000\u0000\uE2FE\uB7A1\uB7A2\u0000\u0000\uB7A3" + // 12120 - 12129
                "\u0000\u0000\u0000\uF5A4\u0000\u0000\u0000\u0000\u0000\uB1AB" + // 12130 - 12139
                "\uB1AC\u0000\u0000\uE9F4\u0000\u0000\u0000\u0000\u0000\u0000" + // 12140 - 12149
                "\uB9F5\u0000\u0000\uE2BB\u0000\uF7AD\u0000\u0000\u0000\uF8E1" + // 12150 - 12159
                "\uB6FA\uB6FB\uB6FC\u0000\u0000\u0000\uB6FD\uB6FE\uB6F5\u0000" + // 12160 - 12169
                "\u0000\u0000\uB6F6\u0000\u0000\u0000\uF6A7\u0000\u0000\u0000" + // 12170 - 12179
                "\uE6FA\u0000\uDCE8\u0000\u0000\u0000\uFAD6\u0000\uD3F6\u0000" + // 12180 - 12189
                "\uA9F3\uA9F4\uA9F5\u0000\u0000\u0000\u0000\u0000\uD5ED\uE7DD" + // 12190 - 12199
                "\u0000\u0000\uF5B6\u0000\u0000\u0000\u0000\u0000\u0000\uD3C1" + // 12200 - 12209
                "\uF0CD\u0000\u0060\u0061\u0062\u0063\u0064\u0065\u0066\u0067" + // 12210 - 12219
                "\uB6EF\uB6F0\u0000\uB6F1\u0000\uB6F2\u0000\u0000\uD8D7\u0000" + // 12220 - 12229
                "\u0000\u0000\u0000\u0000\u0000\uB9A3\u0000\uB9A4\uB6EE\u0000" + // 12230 - 12239
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF6A5\u0000\uE4B1\u0000" + // 12240 - 12249
                "\u0000\u0000\u0000\u0000\u0000\uDCE7\uB6EC\u0000\u0000\u0000" + // 12250 - 12259
                "\uB6ED\u0000\u0000\u0000\uE7B9\u0000\u0000\u0000\u0000\uF0AD" + // 12260 - 12269
                "\uB6E8\u0000\u0000\u0000\uB6E9\u0000\u0000\u0000\uE7D8\u0000" + // 12270 - 12279
                "\u0000\u0000\u0000\u0000\uB1A1\uB1A2\u0000\uB1A3\uB6E4\uB6E5" + // 12280 - 12289
                "\u0000\uB6E6\u0000\u0000\u0000\u0000\uE3FD\u0000\uF9B1\u0000" + // 12290 - 12299
                "\u0000\uEBE9\u0000\u0000\u0000\uE8BB\u0000\u0000\uFBD7\u0000" + // 12300 - 12309
                "\u0000\uEBCA\uE0A1\u0000\u0000\uCFBB\u0000\u0000\u0000\uD3AD" + // 12310 - 12319
                "\uE8E1\uCEEC\uB6E3\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 12320 - 12329
                "\uCACD\u0000\uDFC7\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 12330 - 12339
                "\uE2FD\uB6DF\uB6E0\u0000\u0000\uB6E1\u0000\u0000\uB6E2\uB6DC" + // 12340 - 12349
                "\uB6DD\u0000\u0000\u0000\uB6DE\u0000\u0000\uEEEA\u0000\u0000" + // 12350 - 12359
                "\u0000\uF0E4\uF3B4\uD4EE\uB6DB\u0000\u0000\u0000\u0000\u0000" + // 12360 - 12369
                "\u0000\u0000\uFAF1\u0000\uE1FC\u0000\u0000\u0000\u0000\u0000" + // 12370 - 12379
                "\u0000\u0000\uF9BE\uB6D9\u0000\u0000\u0000\uB6DA\u0000\u0000" + // 12380 - 12389
                "\u0000\uEDD2\u0000\uD4D8\uDCC9\uD7F1\u0000\uE4CD\u0000\uD6B9" + // 12390 - 12399
                "\u0000\u0000\u0000\uEFC0\u0000\uA9EB\uA9EC\uA9ED\uA9EE\uA9EF" + // 12400 - 12409
                "\uA9F0\uA9F1\uA9F2\uB6D3\u0000\u0000\u0000\uB6D4\u0000\u0000" + // 12410 - 12419
                "\u0000\uFCD2\u0000\uEBC8\u0000\u0000\u0000\uF4B4\u0000\u0000" + // 12420 - 12429
                "\u0000\u0000\uF8D5\uE5D8\u0000\u0000\u0000\uFBB9\u0000\uE4D3" + // 12430 - 12439
                "\u0000\uCDF9\uB6D0\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 12440 - 12449
                "\uFDB5\u0000\uD0F7\uEDD4\u0000\u0000\u0000\u0000\u0000\u0000" + // 12450 - 12459
                "\uFAE0\u0000\u0000\uDDEB\u0000\u0000\uE4F9\u0000\u0000\uE3AF" + // 12460 - 12469
                "\uB6CE\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF4DC\u0000" + // 12470 - 12479
                "\uD3F5\u0000\uD7A6\u0000\uF6B5\uD7DB\u0000\u0000\uD0E2\u0000" + // 12480 - 12489
                "\u0000\u0000\u0000\uDDA6\u0000\uA8EF\uA8F0\uA8F1\uA8F2\uA8F3" + // 12490 - 12499
                "\uA8F4\uA8F5\u0000\u0058\u0059\u005A\u005B\\\u005D\u005E" + // 12500 - 12509
                "\u005F\uB6CA\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uFCF7" + // 12510 - 12519
                "\u0000\uCFDD\u0000\u0000\uE8A9\u0000\uE3BB\uE3BA\u0000\uA8E7" + // 12520 - 12529
                "\uA8E8\uA8E9\uA8EA\uA8EB\uA8EC\uA8ED\uA8EE\uB6C7\uB6C8\u0000" + // 12530 - 12539
                "\u0000\uB6C9\u0000\u0000\u0000\uDBB5\u0000\uF3E7\uD8FE\u0000" + // 12540 - 12549
                "\u0000\uE9AB\u0000\u0000\uE1E1\uD3CF\uF4F6\u0000\uCFD1\u0000" + // 12550 - 12559
                "\u0000\u0000\u0000\uCBDC\uCCFA\u0000\uA1C1\uA1D5\u0000\u0000" + // 12560 - 12569
                "\uA1C2\uA1C3\u0000\u0000\uCAA9\u0000\u0000\u0000\u0000\u0000" + // 12570 - 12579
                "\u0000\uE6EE\u0000\uCCDC\uB6C5\u0000\u0000\u0000\u0000\u0000" + // 12580 - 12589
                "\u0000\u0000\uF7A3\u0000\uE3B9\uEBC5\uF4A9\uCDB6\uD2F9\u0000" + // 12590 - 12599
                "\uDAAD\uD2E3\uB6C3\uB6C4\u0000\u0000\u0000\u0000\u0000\u0000" + // 12600 - 12609
                "\uF2C7\u0000\u0000\u0000\uD2FC\u0000\u0000\u0000\u0000\u0000" + // 12610 - 12619
                "\uB0D7\uB0D8\u0000\uB0D9\uB6BE\u0000\u0000\u0000\uB6BF\u0000" + // 12620 - 12629
                "\u0000\u0000\uCDA2\uE8AE\u0000\u0000\u0000\uE1BD\uB6B6\uB6B7" + // 12630 - 12639
                "\u0000\uB6B8\uB6B9\uB6BA\u0000\u0000\uD4ED\uE2C4\u0000\u0000" + // 12640 - 12649
                "\u0000\u0000\uE9E7\uB6B3\u0000\uB6B4\uB6B5\u0000\u0000\u0000" + // 12650 - 12659
                "\u0000\uF4E6\u0000\u0000\uE6C5\uEFD5\uB6B0\uB6B1\u0000\u0000" + // 12660 - 12669
                "\uB6B2\u0000\u0000\u0000\uECEE\u0000\u0000\uDDAA\u0000\u0000" + // 12670 - 12679
                "\uF1CE\uF2E4\u0000\u0000\uD0B0\u0000\u0000\uCDC6\uF2B6\u0000" + // 12680 - 12689
                "\u0000\uDDFE\u0000\u0000\uD9AA\u0000\u0000\u0000\u0000\u0000" + // 12690 - 12699
                "\u0000\uE7E2\u0000\u0000\uE9FB\uEAA8\u0000\u0000\u0000\u0000" + // 12700 - 12709
                "\uFDB7\uB6AE\uB6AF\u0000\u0000\u0000\u0000\u0000\u0000\uF0A6" + // 12710 - 12719
                "\u0000\u0000\u0000\uDCC8\u0000\u0000\u0000\u0000\u0000\uB0C2" + // 12720 - 12729
                "\u0000\u0000\u0000\uD7B0\uD8E8\uCBBD\u0000\u0000\uEECF\uF7D7" + // 12730 - 12739
                "\u0000\u0000\uE0A6\uD6C1\uE1DC\uB6A9\u0000\u0000\u0000\uB6AA" + // 12740 - 12749
                "\u0000\u0000\u0000\uEAB8\uD1F9\u0000\u0000\u0000\u0000\uC4E8" + // 12750 - 12759
                "\u0000\u0000\u0000\uE7A2\uE4D9\u0000\u0000\u0000\uF0E6\u0000" + // 12760 - 12769
                "\u0000\u0000\uE4B9\uB6A1\uB6A2\u0000\uB6A3\uB6A4\uB6A5\u0000" + // 12770 - 12779
                "\u0000\uEEE9\u0000\u0000\u0000\uF5DA\u0000\u0000\uCFBE\u0000" + // 12780 - 12789
                "\u0000\uECBB\u0000\u0000\u0000\uEEED\u0000\u0000\u0000\uECEB" + // 12790 - 12799
                "\uDEC5\uB5FE\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE4AF" + // 12800 - 12809
                "\u0000\uDBAB\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE6B4" + // 12810 - 12819
                "\uB5FB\uB5FC\u0000\u0000\uB5FD\u0000\u0000\u0000\uF1BE\u0000" + // 12820 - 12829
                "\u0000\uD3AC\u0000\u0000\uD0CE\u0000\u0000\uDAF7\u0000\u0000" + // 12830 - 12839
                "\u0000\uD9D3\u0000\u0000\u0000\u0000\uD3DE\uB5F8\uB5F9\uB5FA" + // 12840 - 12849
                "\u0000\u0000\u0000\u0000\u0000\uBBD5\u0000\u0000\uBBD6\uB5F2" + // 12850 - 12859
                "\u0000\u0000\uB5F3\uB5F4\u0000\u0000\u0000\uE0C2\u0000\uCAE4" + // 12860 - 12869
                "\u0000\uE7B7\u0000\uEEF6\uEACF\uF0EE\uE3FC\u0000\uD3DF\uD3F4" + // 12870 - 12879
                "\uE1B3\uB5EF\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF6E4" + // 12880 - 12889
                "\u0000\uF5BB\u0000\uDED1\u0000\u0000\u0000\u0000\u0000\uF6DD" + // 12890 - 12899
                "\u0000\uF1A3\u0000\uA1FD\uA1FB\uA1FA\uA1F2\uA1F3\u0000\uA2B1" + // 12900 - 12909
                "\u0000\u0050\u0051\u0052\u0053\u0054\u0055\u0056\u0057\uB5E7" + // 12910 - 12919
                "\u0000\u0000\uB5E8\uB5E9\u0000\uB5EA\u0000\uF3F3\uE3FB\u0000" + // 12920 - 12929
                "\uDED0\uCEB0\u0000\uD6F7\uF1D9\uB5E3\u0000\u0000\u0000\u0000" + // 12930 - 12939
                "\uB5E4\u0000\u0000\uEFAE\u0000\u0000\u0000\uF4D0\uCEF3\u0000" + // 12940 - 12949
                "\uD8DB\u0000\uF9CE\uE9D5\uE3D1\u0000\u0000\uD2BC\uB5E2\u0000" + // 12950 - 12959
                "\u0000\u0000\u0000\u0000\u0000\u0000\uD7DA\u0000\uF3F2\u0000" + // 12960 - 12969
                "\uEED6\uEAB2\uD0F6\uECD9\uDACB\uCFA8\uB5E0\u0000\u0000\u0000" + // 12970 - 12979
                "\uB5E1\u0000\u0000\u0000\uE4FB\u0000\u0000\uF9E4\u0000\u0000" + // 12980 - 12989
                "\uDBD4\uD7C7\u0000\u0000\u0000\u0000\uF2FE\uB5DB\u0000\u0000" + // 12990 - 12999
                "\u0000\uB5DC\u0000\u0000\u0000\uCCBD\u0000\u0000\uD1A9\uDDCC" + // 13000 - 13009
                "\u0000\uE3E4\uE9BB\u0000\u0000\u0000\u0000\u0000\uE2D6\uB5D8" + // 13010 - 13019
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uFAB6\u0000\uEEA5" + // 13020 - 13029
                "\u0000\u0000\uFAAA\uE6C3\uE1B2\uCAAB\u0000\uA1D0\u0000\u0000" + // 13030 - 13039
                "\u0000\u0000\uA1AB\u0000\uA1FC\uB5D7\u0000\u0000\u0000\u0000" + // 13040 - 13049
                "\u0000\u0000\u0000\uE5A7\uD5D2\uB5D2\uB5D3\u0000\uB5D4\u0000" + // 13050 - 13059
                "\uB5D5\u0000\u0000\uEEB0\u0000\u0000\u0000\u0000\u0000\u0000" + // 13060 - 13069
                "\uB8D2\u0000\u0000\uCEEE\u0000\u0000\uECCF\u0000\u0000\u0000" + // 13070 - 13079
                "\uD0D1\uCBBF\u0000\uEDA4\u0000\u0000\uD4A9\u0000\u0000\u0000" + // 13080 - 13089
                "\u0000\uCDC2\uE7DA\uB5D1\u0000\u0000\u0000\u0000\u0000\u0000" + // 13090 - 13099
                "\u0000\uCCCB\u0000\uCEAF\uF1B5\uEFD2\uE8C8\uEBF1\u0000\u0000" + // 13100 - 13109
                "\u0000\uE7C5\u0000\u0000\uE0E9\u0000\uA1F4\u0000\u0000\uA1F5" + // 13110 - 13119
                "\u0000\u0000\u0000\uA2B3\uB5CE\uB5CF\u0000\u0000\uB5D0\u0000" + // 13120 - 13129
                "\u0000\u0000\uE7FD\u0000\u0000\uE6A3\uFBF1\uCBB0\uB5CA\uB5CB" + // 13130 - 13139
                "\u0000\uB5CC\u0000\u0000\u0000\u0000\uCDB7\u0000\uEFD4\u0000" + // 13140 - 13149
                "\u0000\uF0E2\u0000\u0000\u0000\u0000\u0000\uEECE\uB5C9\u0000" + // 13150 - 13159
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE1F3\uDCF6\uB5C7\u0000" + // 13160 - 13169
                "\u0000\u0000\uB5C8\u0000\u0000\u0000\uD4B8\uEBBE\uDDEF\u0000" + // 13170 - 13179
                "\uDDF0\uDDF1\uB5C6\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 13180 - 13189
                "\uD9B2\uFDA5\uB5C4\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 13190 - 13199
                "\uD3A2\u0000\uE1FA\uE4CC\u0000\uE1E4\uE8C7\u0000\u0000\uCEDB" + // 13200 - 13209
                "\uB5C2\u0000\u0000\u0000\uB5C3\u0000\u0000\u0000\uDFAF\u0000" + // 13210 - 13219
                "\uCAC3\u0000\u0000\uEEFC\uB5BB\u0000\u0000\u0000\uB5BC\uB5BD" + // 13220 - 13229
                "\u0000\uB5BE\uB5B7\u0000\u0000\uB5B8\uB5B9\u0000\uB5BA\u0000" + // 13230 - 13239
                "\uF9CA\u0000\uEAE8\u0000\uE5ED\u0000\u0000\u0000\uDCEE\u0000" + // 13240 - 13249
                "\u0000\uF5EA\uE6E0\uB5B3\u0000\u0000\u0000\uB5B4\u0000\u0000" + // 13250 - 13259
                "\u0000\uE0E8\u0000\u0000\uD3AB\u0000\uEBDC\uB5B1\uB5B2\u0000" + // 13260 - 13269
                "\u0000\u0000\u0000\u0000\u0000\uF8A6\u0000\uDECA\uF2C6\uB5AF" + // 13270 - 13279
                "\u0000\u0000\u0000\uB5B0\u0000\u0000\u0000\uE1A8\u0000\u0000" + // 13280 - 13289
                "\u0000\u0000\uD5F6\uB5A9\uB5AA\u0000\uB5AB\uB5AC\uB5AD\u0000" + // 13290 - 13299
                "\u0000\uEEE6\u0000\uE0D3\u0000\u0000\u0000\u0000\uF3D1\u0000" + // 13300 - 13309
                "\u0000\u0000\u0000\uBFD6\uBFD7\u0000\u0000\uD7E7\u0000\u0000" + // 13310 - 13319
                "\u0000\u0000\u0000\u0000\uE9EA\uD7CC\u0000\uE2D5\uEDCF\u0000" + // 13320 - 13329
                "\u0000\u0000\uDDA2\u0000\u0000\uCAF1\u0000\u0000\u0000\u0000" + // 13330 - 13339
                "\u0000\u0000\uF4D8\uD6B3\uDDAD\uB5A8\u0000\u0000\u0000\u0000" + // 13340 - 13349
                "\u0000\u0000\u0000\uCBEB\u0000\uF0ED\u0000\uDDA1\u0000\uEDAF" + // 13350 - 13359
                "\uFCF8\u0000\uD8EB\uB5A5\uB5A6\u0000\u0000\uB5A7\u0000\u0000" + // 13360 - 13369
                "\u0000\uE8E0\u0000\u0000\u0000\u0000\u0000\uCFD5\uD8FD\u0000" + // 13370 - 13379
                "\u0000\uF0E1\u0000\u0000\u0000\u0000\u0000\u0000\uB7E4\u0000" + // 13380 - 13389
                "\uB7E5\uB4F8\u0000\u0000\uB4F9\uB4FA\u0000\uB4FB\uB4FC\uB4EF" + // 13390 - 13399
                "\uB4F0\u0000\uB4F1\uB4F2\uB4F3\u0000\u0000\uF2E1\u0000\uDEB9" + // 13400 - 13409
                "\u0000\u0000\u0000\u0000\uD5F4\u0000\u0000\u0000\uCDED\uB4EE" + // 13410 - 13419
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCED2\u0000\uEDAD" + // 13420 - 13429
                "\uFAEA\u0000\u0000\uCDEE\uEDA6\u0000\uEDAE\uB4EB\uB4EC\u0000" + // 13430 - 13439
                "\u0000\uB4ED\u0000\u0000\u0000\uD1A7\u0000\u0000\uFDE3\uCEB3" + // 13440 - 13449
                "\u0000\uDFC5\u0000\u0000\uE5BE\u0000\u0000\u0000\u0000\uC1CB" + // 13450 - 13459
                "\u0000\u0000\u0000\uDADA\u0000\uF2DC\uFBD6\uE9B2\uB4E6\uB4E7" + // 13460 - 13469
                "\uB4E8\uB4E9\u0000\u0000\u0000\uB4EA\uB4DC\u0000\u0000\uB4DD" + // 13470 - 13479
                "\uB4DE\uB4DF\uB4E0\uB4E1\uB4D4\uB4D5\u0000\uB4D6\u0000\uB4D7" + // 13480 - 13489
                "\u0000\u0000\uF8FB\uE3CF\u0000\u0000\u0000\u0000\u0000\uB6F3" + // 13490 - 13499
                "\uB6F4\u0000\u0000\uE2B9\u0000\u0000\u0000\u0000\u0000\u0000" + // 13500 - 13509
                "\uF3C8\u0000\uF3BA\uB4D2\u0000\uB4D3\u0000\u0000\u0000\u0000" + // 13510 - 13519
                "\u0000\uBBD1\uBBD2\u0000\u0000\uDFDA\u0000\u0000\u0000\u0000" + // 13520 - 13529
                "\u0000\u0000\uB7D8\u0000\u0000\uDBE5\u0000\uDDDE\u0000\u0000" + // 13530 - 13539
                "\uD9F0\uE9A3\uB4CF\uB4D0\u0000\u0000\uB4D1\u0000\u0000\u0000" + // 13540 - 13549
                "\uE9B0\u0000\u0000\u0000\u0000\u0000\uCDAE\u0000\u0000\u0000" + // 13550 - 13559
                "\uE6E7\u0000\u0000\uEAC7\u0000\uE3E3\u0000\u0000\u0000\u0000" + // 13560 - 13569
                "\uE4B0\u0000\u0000\uF5C3\uE9D8\u0000\u0000\u0000\u0000\u0000" + // 13570 - 13579
                "\uF1D1\u0000\u0000\u0000\uF5FE\uD4AC\u0000\u0000\u0000\uCAE7" + // 13580 - 13589
                "\u0000\u0000\u0000\u0000\uC8DE\uC8DF\u0000\u0000\uCCA8\u0000" + // 13590 - 13599
                "\uDAC1\uCCD5\u0000\uD9E4\u0000\uA2A3\u0000\uA1D3\uA2A4\u0000" + // 13600 - 13609
                "\u0000\u0000\uA1D4\uB4CD\u0000\u0000\u0000\uB4CE\u0000\u0000" + // 13610 - 13619
                "\u0000\uD5B2\u0000\u0000\u0000\uD5BC\u0000\uD2A6\u0000\u0000" + // 13620 - 13629
                "\uE7F4\uD1D6\u0000\u0000\uE6C2\uB4C6\uB4C7\u0000\uB4C8\u0000" + // 13630 - 13639
                "\uB4C9\uB4CA\u0000\uE6C1\u0000\u0000\uECD8\u0000\u0000\u0000" + // 13640 - 13649
                "\uEDAC\uB4C3\uB4C4\uB4C5\u0000\u0000\u0000\u0000\u0000\uBBCE" + // 13650 - 13659
                "\u0000\u0000\u0000\uD5EA\uF1EE\u0000\u0000\u0000\u0000\uC3F2" + // 13660 - 13669
                "\u0000\u0000\u0000\uECA2\uEDFD\u0000\uF5B4\uFBB8\uB4C0\uB4C1" + // 13670 - 13679
                "\u0000\u0000\uB4C2\u0000\u0000\u0000\uD4A2\uCFF6\u0000\u0000" + // 13680 - 13689
                "\u0000\u0000\uC3E8\u0000\u0000\u0000\uE3F4\uCDD0\u0000\u0000" + // 13690 - 13699
                "\u0000\uD5B8\u0000\u0000\uF7FD\u0000\uECA9\u0000\uF2EB\u0000" + // 13700 - 13709
                "\uFDEF\u0000\uF9F3\u0000\uA2D9\uA2D7\u0000\u0000\u0000\u0000" + // 13710 - 13719
                "\u0000\u0000\uFCC8\u0000\u0000\uEAE7\uDFC3\uD1D2\uCEE2\u0000" + // 13720 - 13729
                "\uD3A4\u0000\u0048\u0049\u004A\u004B\u004C\u004D\u004E\u004F" + // 13730 - 13739
                "\uB4B8\uB4B9\u0000\u0000\u0000\u0000\u0000\u0000\uFBFA\u0000" + // 13740 - 13749
                "\uCFA4\u0000\uEEBB\uCDB4\u0000\uE0F3\uEACD\u0000\u0000\u0000" + // 13750 - 13759
                "\uDAF2\u0000\u0000\u0000\u0000\uC6CA\uC6CB\u0000\uC6CC\uB4B7" + // 13760 - 13769
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uA2CF\uA2CE\uB4B5" + // 13770 - 13779
                "\u0000\u0000\u0000\uB4B6\u0000\u0000\u0000\uE9F8\uE2E5\u0000" + // 13780 - 13789
                "\u0000\u0000\u0000\uC3D2\u0000\u0000\u0000\uD5E0\uF6CA\uFDCA" + // 13790 - 13799
                "\uD8D6\uF4CF\uB4B2\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 13800 - 13809
                "\uA8AE\uA9AE\uB4AB\u0000\u0000\uB4AC\uB4AD\u0000\u0000\u0000" + // 13810 - 13819
                "\uDBE0\u0000\u0000\u0000\u0000\u0000\uF9CD\u0000\u0000\u0000" + // 13820 - 13829
                "\uF7F5\u0000\uCBE5\u0000\u0000\uFCC3\u0000\u0000\u0000\u0000" + // 13830 - 13839
                "\uD4E7\u0000\uDBAA\u0000\u0000\u0000\uF7E0\u0000\u0000\u0000" + // 13840 - 13849
                "\uD7BC\uCCE3\u0000\u0000\uE6DB\uB4A5\u0000\u0000\u0000\u0000" + // 13850 - 13859
                "\u0000\u0000\u0000\uA8A4\uA9A4\uB4A2\uB4A3\u0000\u0000\uB4A4" + // 13860 - 13869
                "\u0000\u0000\u0000\uE4C5\u0000\u0000\u0000\u0000\u0000\uDDDF" + // 13870 - 13879
                "\u0000\u0000\u0000\uDBA2\uF2F6\u0000\u0000\uCABA\uB3FB\u0000" + // 13880 - 13889
                "\u0000\u0000\uB3FC\u0000\u0000\u0000\uE5C5\u0000\u0000\u0000" + // 13890 - 13899
                "\u0000\u0000\uD8AA\u0000\u0000\u0000\uF6AC\u0000\u0000\u0000" + // 13900 - 13909
                "\u0000\uE4ED\u0000\u0000\u0000\u0000\uBFB0\uBFB1\uBFB2\uBFB3" + // 13910 - 13919
                "\uB3F9\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uA9A1\u0000" + // 13920 - 13929
                "\uCACC\u0000\u0000\u0000\u0000\uFBBF\u0000\u0000\uF4CB\u0000" + // 13930 - 13939
                "\u0000\u0000\uFDC5\u0000\u0000\uECF6\uE2E1\uE3BE\u0000\u0000" + // 13940 - 13949
                "\u0000\u0000\uC2B9\u0000\u0000\u0000\uEEE2\u0000\u0000\u0000" + // 13950 - 13959
                "\u0000\uC8C4\uC8C5\u0000\u0000\uE5BF\u0000\u0000\u0000\uCEBF" + // 13960 - 13969
                "\u0000\u0000\uE3FA\u0000\u0000\uF0AA\uF9D0\u0000\u0000\uFBE3" + // 13970 - 13979
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE8A4\u0000\u0000\uE5A9" + // 13980 - 13989
                "\uE0F6\uF6B3\u0000\u0000\u0000\u0000\uC2BC\uC2BD\u0000\u0000" + // 13990 - 13999
                "\uE1FE\u0000\u0000\u0000\u0000\uCBF0\u0000\uA1E7\uA1E8\uA1E6" + // 14000 - 14009
                "\uA1E9\uA1EA\uA2D5\uA2D8\uA2D6\uB3F7\u0000\u0000\u0000\uB3F8" + // 14010 - 14019
                "\u0000\u0000\u0000\uF5A1\u0000\u0000\u0000\u0000\u0000\uD0A1" + // 14020 - 14029
                "\u0000\u0000\u0000\uFAD3\u0000\u0000\u0000\u0000\uFDB3\u0000" + // 14030 - 14039
                "\uD5E4\u0000\u0000\uFCA6\u0000\u0000\u0000\u0000\u0000\u0000" + // 14040 - 14049
                "\uB6CB\u0000\u0000\uF0FD\u0000\u0000\u0000\u0000\u0000\uD7AC" + // 14050 - 14059
                "\uB3F0\uB3F1\u0000\uB3F2\u0000\uB3F3\u0000\u0000\uD0CD\u0000" + // 14060 - 14069
                "\u0000\u0000\u0000\u0000\u0000\uB5DD\u0000\uB5DE\uB3EE\u0000" + // 14070 - 14079
                "\uB3EF\u0000\u0000\u0000\u0000\u0000\uBBBE\u0000\u0000\u0000" + // 14080 - 14089
                "\uDCEA\u0000\u0000\uF0F7\u0000\uF0CA\uB3EB\uB3EC\u0000\u0000" + // 14090 - 14099
                "\uB3ED\u0000\u0000\u0000\uF1BB\u0000\u0000\u0000\u0000\uE9F1" + // 14100 - 14109
                "\uB3EA\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uA8A1\u0000" + // 14110 - 14119
                "\uEEF5\u0000\uDECE\u0000\u0000\u0000\u0000\uE7F3\uB3E8\u0000" + // 14120 - 14129
                "\u0000\u0000\uB3E9\u0000\u0000\u0000\uE2E3\uEEFB\u0000\u0000" + // 14130 - 14139
                "\uDFF7\uD7CA\uB3E4\uB3E5\u0000\u0000\uB3E6\uB3E7\u0000\u0000" + // 14140 - 14149
                "\uCCC5\uECD0\uCBBB\u0000\uDEF3\u0000\u0000\uEEAD\u0000\uFAE3" + // 14150 - 14159
                "\u0000\u0000\u0000\u0000\uC7D8\uC7D9\u0000\u0000\uE1C4\u0000" + // 14160 - 14169
                "\u0000\u0000\u0000\u0000\uE8B0\uB3E3\u0000\u0000\u0000\u0000" + // 14170 - 14179
                "\u0000\u0000\u0000\uC8B1\uB3E0\uB3E1\u0000\u0000\uB3E2\u0000" + // 14180 - 14189
                "\u0000\u0000\uEAF6\u0000\u0000\uF6F9\u0000\u0000\uFDC9\u0000" + // 14190 - 14199
                "\u0000\u0000\u0000\uE4A6\uF9A4\uB3DE\uB3DF\u0000\u0000\u0000" + // 14200 - 14209
                "\u0000\u0000\u0000\uE4F8\u0000\u0000\u0000\uD8DD\u0000\uCDFD" + // 14210 - 14219
                "\uF2AB\u0000\u0000\uE3CD\u0000\u0000\u0000\u0000\uF4F4\uFAB2" + // 14220 - 14229
                "\uB3D9\u0000\u0000\u0000\uB3DA\u0000\u0000\u0000\uCFF5\u0000" + // 14230 - 14239
                "\u0000\uFDAE\u0000\u0000\uDEA6\u0000\u0000\uEBFE\u0000\uEBDD" + // 14240 - 14249
                "\uF0E0\uB3D1\uB3D2\u0000\uB3D3\uB3D4\uB3D5\u0000\u0000\uF4DA" + // 14250 - 14259
                "\u0000\u0000\u0000\u0000\u0000\u0000\uB5D9\u0000\u0000\uCBB5" + // 14260 - 14269
                "\uD8D1\u0000\uF4CE\uF3F7\u0000\u0000\uEBF7\u0000\u0000\u0000" + // 14270 - 14279
                "\u0000\u0000\u0000\uE9B3\u0000\u0000\uEFE8\u0000\u0000\uE1BF" + // 14280 - 14289
                "\u0000\u0000\u0000\uEAA4\u0000\u0000\u0000\uFAC2\uB3CE\u0000" + // 14290 - 14299
                "\uB3CF\uB3D0\u0000\u0000\u0000\u0000\uEDE3\u0000\u0000\uD3E1" + // 14300 - 14309
                "\u0000\uFCB1\uCCF8\u0000\u0000\uDDC6\uFAD1\u0000\uF7DF\uB3CA" + // 14310 - 14319
                "\uB3CB\u0000\uB3CC\uB3CD\u0000\u0000\u0000\uEDE7\uFBB5\uF8EC" + // 14320 - 14329
                "\u0000\u0000\u0000\uF4EB\u0000\uEEB5\u0000\uF5D8\uB3C8\u0000" + // 14330 - 14339
                "\u0000\u0000\u0000\uB3C9\u0000\u0000\uE3CC\u0000\u0000\u0000" + // 14340 - 14349
                "\uCFF8\uEFAC\u0000\uD2AF\uDCE5\u0000\u0000\u0000\u0000\uD0A5" + // 14350 - 14359
                "\uF1B4\uB3C7\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uC5F2" + // 14360 - 14369
                "\uB3C4\uB3C5\u0000\u0000\uB3C6\u0000\u0000\u0000\uDCB6\uE4E9" + // 14370 - 14379
                "\u0000\u0000\u0000\u0000\uC2EE\uC2EF\u0000\u0000\uFAC9\u0000" + // 14380 - 14389
                "\u0000\uE1CD\u0000\uCAB8\u0000\uD7A4\uCEC5\u0000\u0000\u0000" + // 14390 - 14399
                "\u0000\uCED5\uD6E6\uB3C2\uB3C3\u0000\u0000\u0000\u0000\u0000" + // 14400 - 14409
                "\u0000\uF4A8\u0000\uDCF8\u0000\uD9F8\uD4C2\u0000\u0000\u0000" + // 14410 - 14419
                "\u0000\uF6E5\u0000\uA5A9\uA5AA\u0000\u0000\u0000\u0000\u0000" + // 14420 - 14429
                "\u0000\uFBEC\u0000\uDDC8\uB3BD\u0000\u0000\u0000\uB3BE\u0000" + // 14430 - 14439
                "\u0000\u0000\uE8DA\uDAC3\uDAC4\uD4C5\u0000\uE7FA\uB3B2\uB3B3" + // 14440 - 14449
                "\u0000\uB3B4\uB3B5\uB3B6\uB3B7\uB3B8\uB3AF\uB3B0\uB3B1\u0000" + // 14450 - 14459
                "\u0000\u0000\u0000\u0000\uBBAD\uBBAE\u0000\uBBAF\uB3AA\uB3AB" + // 14460 - 14469
                "\uB3AC\u0000\uB3AD\u0000\u0000\uB3AE\uB3A4\u0000\u0000\u0000" + // 14470 - 14479
                "\uB3A5\u0000\u0000\u0000\uD7B2\u0000\u0000\u0000\u0000\uD0FD" + // 14480 - 14489
                "\uB2F6\u0000\uB2F7\u0000\uB2F8\u0000\uB2F9\u0000\uD3D2\u0000" + // 14490 - 14499
                "\uF5C0\u0000\u0000\u0000\uDFDD\u0000\uA5A1\uA5A2\uA5A3\uA5A4" + // 14500 - 14509
                "\uA5A5\uA5A6\uA5A7\uA5A8\uB2F3\u0000\u0000\u0000\u0000\u0000" + // 14510 - 14519
                "\u0000\u0000\uB7AD\uB2EF\u0000\u0000\u0000\uB2F0\u0000\u0000" + // 14520 - 14529
                "\u0000\uD6B0\uF8CA\u0000\uFCFA\u0000\uD9FE\uB2EB\uB2EC\u0000" + // 14530 - 14539
                "\u0000\uB2ED\u0000\u0000\u0000\uCDBB\u0000\uEFDA\uEED8\u0000" + // 14540 - 14549
                "\uDDA7\uB2EA\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uB2BB" + // 14550 - 14559
                "\uB2E7\uB2E8\u0000\u0000\uB2E9\u0000\u0000\u0000\uCAF2\uDFA4" + // 14560 - 14569
                "\u0000\u0000\uD4C4\u0000\uE5E8\uDCC3\u0000\u0000\uEDDE\uD3F2" + // 14570 - 14579
                "\u0000\u0000\uF3F4\uF8F3\uF0C1\uDEAF\uF8B0\u0000\u0000\uE6B5" + // 14580 - 14589
                "\u0000\u0000\uF9A8\u0000\u0000\uDDD8\uB2E5\uB2E6\u0000\u0000" + // 14590 - 14599
                "\u0000\u0000\u0000\u0000\uD7CF\uEBEA\uFDEB\u0000\uCBA8\uEBBC" + // 14600 - 14609
                "\uE4BE\u0000\u0000\u0000\u0000\u0000\uF9DD\uEABF\u0000\u0000" + // 14610 - 14619
                "\uD2C0\u0000\u0000\u0000\u0000\u0000\u0000\uCAD8\u0000\u0000" + // 14620 - 14629
                "\uE5B0\u0000\u0000\u0000\uEDE5\u0000\u0000\uFDC4\u0000\uECAD" + // 14630 - 14639
                "\u0000\u0000\u0000\u0000\uC1ED\u0000\u0000\u0000\uCAB4\u0000" + // 14640 - 14649
                "\u0000\uF8E2\uCFC2\uB2DE\uB2DF\u0000\uB2E0\u0000\uB2E1\uB2E2" + // 14650 - 14659
                "\u0000\uCFDC\u0000\uD3D1\u0000\u0000\uCCB1\uF7D8\u0000\uA5B8" + // 14660 - 14669
                "\uA5B9\u0000\u0000\u0000\u0000\u0000\u0000\uD2B7\u0000\u0000" + // 14670 - 14679
                "\uF7A2\u0000\u0000\u0000\u0000\u0000\u0000\uF5A3\u0000\u0000" + // 14680 - 14689
                "\uCDB2\u0000\uDAAB\u0000\uCAA7\u0000\u0000\uEAAC\u0000\u0000" + // 14690 - 14699
                "\u0000\uCAA8\u0000\u0000\uF3DD\u0000\u0000\u0000\uE4DA\u0000" + // 14700 - 14709
                "\u0000\uFDAA\uF9E2\u0000\u0000\u0000\u0000\u0000\uF2BE\uF6A1" + // 14710 - 14719
                "\u0000\uEBCB\uB2DC\u0000\u0000\u0000\u0000\u0000\u0000\uB2DD" + // 14720 - 14729
                "\uB2D9\uB2DA\u0000\u0000\uB2DB\u0000\u0000\u0000\uCEE6\uFCAB" + // 14730 - 14739
                "\uD5BB\u0000\u0000\uF2A8\uB2D5\uB2D6\u0000\u0000\u0000\uB2D7" + // 14740 - 14749
                "\u0000\u0000\uCBBA\u0000\u0000\uE5D1\u0000\u0000\u0000\uF8D4" + // 14750 - 14759
                "\u0000\u0000\u0000\u0000\u0000\uD6F3\u0000\u0000\u0000\uD8ED" + // 14760 - 14769
                "\uE3C4\uF0F1\u0000\u0000\uD5A9\uFAE2\u0000\u0000\u0000\uD0E5" + // 14770 - 14779
                "\u0000\uF3F1\u0000\u0000\u0000\uE3D0\u0000\u0000\uF2FB\uB2D4" + // 14780 - 14789
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uB1CC\uB2D2\u0000" + // 14790 - 14799
                "\u0000\u0000\uB2D3\u0000\u0000\u0000\uF7EC\u0000\u0000\u0000" + // 14800 - 14809
                "\uE8F6\u0000\uDCFC\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 14810 - 14819
                "\uE1B0\uB2CC\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE4CB" + // 14820 - 14829
                "\uB2CA\uB2CB\u0000\u0000\u0000\u0000\u0000\u0000\uD4DF\u0000" + // 14830 - 14839
                "\u0000\u0000\uF4AD\u0000\uFCAA\u0000\u0000\u0000\uFAB0\u0000" + // 14840 - 14849
                "\u0000\u0000\u0000\uE5A6\u0000\u0000\u0000\u0000\uBEE4\uBEE5" + // 14850 - 14859
                "\u0000\uBEE6\uB2C1\u0000\uB2C2\u0000\uB2C3\u0000\u0000\u0000" + // 14860 - 14869
                "\uD0D3\u0000\uD3BD\u0000\u0000\u0000\uCCB8\u0000\u0000\u0000" + // 14870 - 14879
                "\uD0AE\uB2BE\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uEED2" + // 14880 - 14889
                "\uB2BC\u0000\u0000\u0000\u0000\uB2BD\u0000\u0000\uCFD9\u0000" + // 14890 - 14899
                "\u0000\uDCCD\uEDFB\u0000\uDEF0\uB2B9\u0000\u0000\u0000\uB2BA" + // 14900 - 14909
                "\u0000\u0000\u0000\uCCAA\u0000\u0000\uF0C3\uCCD6\u0000\uF1D8" + // 14910 - 14919
                "\u0000\u0000\uD8D8\u0000\u0000\uE0F2\u0000\uA5B0\uA5B1\uA5B2" + // 14920 - 14929
                "\uA5B3\uA5B4\uA5B5\uA5B6\uA5B7\uB2B5\u0000\u0000\uB2B6\u0000" + // 14930 - 14939
                "\uB2B7\u0000\u0000\uEEAE\uD6AE\u0000\u0000\u0000\u0000\u0000" + // 14940 - 14949
                "\uB5F0\uB5F1\u0000\u0000\uDDCF\u0000\u0000\u0000\u0000\u0000" + // 14950 - 14959
                "\u0000\uF4D1\u0000\u0000\uF2D4\u0000\uD1B0\u0000\uCCE0\u0000" + // 14960 - 14969
                "\uDBFD\uB2B2\uB2B3\u0000\u0000\uB2B4\u0000\u0000\u0000\uD2B5" + // 14970 - 14979
                "\u0000\u0000\u0000\uD3D5\u0000\uEBD9\u0000\uCFA7\uEAAF\u0000" + // 14980 - 14989
                "\u0000\u0000\u0000\uC0F7\u0000\u0000\u0000\uE8CD\u0000\u0000"   // 14990 - 14999
                ;

            index2a =
                "\u0000\u0000\uC5D7\uC5D8\u0000\u0000\uE3BD\u0000\uCFE1\uF0C0" + // 15000 - 15009
                "\uECDA\u0000\uDDD7\uB2B0\uB2B1\u0000\u0000\u0000\u0000\u0000" + // 15010 - 15019
                "\u0000\uF5CA\uE9B6\u0000\u0000\uEABE\uD9B1\u0000\u0000\u0000" + // 15020 - 15029
                "\u0000\u0000\uB5EB\uB5EC\u0000\uB5ED\uB2AB\u0000\u0000\u0000" + // 15030 - 15039
                "\uB2AC\u0000\u0000\u0000\uFDCD\u0000\u0000\u0000\uF3B6\u0000" + // 15040 - 15049
                "\uD3DC\u0000\u0000\uFAFE\u0000\u0000\u0000\u0000\uC0E7\uC0E8" + // 15050 - 15059
                "\u0000\u0000\uF2ED\u0000\uDBD9\u0000\uF0A8\u0000\u0000\uDBDF" + // 15060 - 15069
                "\uD3D3\uF8C7\u0000\u0000\u0000\u0000\uC1E1\u0000\u0000\u0000" + // 15070 - 15079
                "\uF9FD\u0000\uCADC\u0000\u0000\uDCA7\u0000\u0000\uD6E7\u0000" + // 15080 - 15089
                "\u0000\u0000\uF2B8\uF6C8\u0000\u0000\u0000\uF4BF\uE2EF\u0000" + // 15090 - 15099
                "\uD9F1\uF1C7\uB1FE\uB2A1\u0000\uB2A2\uB2A3\uB2A4\u0000\u0000" + // 15100 - 15109
                "\uFBAC\uCFC3\uEBFD\u0000\u0000\u0000\u0000\uCAE1\u0000\uD4CC" + // 15110 - 15119
                "\u0000\u0000\uDEE8\u0000\u0000\u0000\u0000\u0000\u0000\uE0D6" + // 15120 - 15129
                "\u0000\u0000\uD2C5\uFBD1\uE7C0\uEBA5\u0000\uDFFA\uE3A2\uB1FD" + // 15130 - 15139
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE8A8\uB1FA\uB1FB" + // 15140 - 15149
                "\u0000\u0000\uB1FC\u0000\u0000\u0000\uF2CA\u0000\u0000\u0000" + // 15150 - 15159
                "\u0000\u0000\uCCB0\uEAA2\u0000\u0000\uE4B7\u0000\uEADB\u0000" + // 15160 - 15169
                "\uF5FA\u0000\u0000\uF5E9\u0000\u0000\u0000\u0000\u0000\u0000" + // 15170 - 15179
                "\uCBF5\u0000\u0000\uD7B8\u0000\u0000\u0000\u0000\u0000\u0000" + // 15180 - 15189
                "\uE3B2\u0000\u0000\uDAFE\u0000\u0000\uCCBE\u0000\u0000\uF2FC" + // 15190 - 15199
                "\uB1F7\uB1F8\u0000\u0000\u0000\uB1F9\u0000\u0000\uCDCC\u0000" + // 15200 - 15209
                "\u0000\u0000\u0000\uEDD9\u0000\uCCD0\u0000\u0000\u0000\u0000" + // 15210 - 15219
                "\uCFA6\u0000\u0000\uF7B6\u0000\u0000\u0000\u0000\uF4DE\u0000" + // 15220 - 15229
                "\uA2B6\u0000\uA1C7\uA1C8\u0000\u0000\u0000\u0000\uBDF7\uBDF8" + // 15230 - 15239
                "\u0000\u0000\uDDC3\u0000\uF9DF\u0000\u0000\u0000\u0000\uBFE8" + // 15240 - 15249
                "\uBFE9\u0000\uBFEA\uB1F1\u0000\u0000\u0000\uB1F2\u0000\uB1F3" + // 15250 - 15259
                "\u0000\uF1A6\uCBD5\u0000\u0000\u0000\u0000\u0000\u0000\uE3A5" + // 15260 - 15269
                "\u0000\u0000\uD0F9\uECAB\uDED3\uF7E9\u0000\u0000\uF9F5\uB1E8" + // 15270 - 15279
                "\uB1E9\u0000\uB1EA\u0000\uB1EB\uB1EC\u0000\uF2A3\u0000\uF7F8" + // 15280 - 15289
                "\u0000\u0000\u0000\u0000\uD0B3\uB1E6\u0000\uB1E7\u0000\u0000" + // 15290 - 15299
                "\u0000\u0000\u0000\uBAF6\uBAF7\u0000\uBAF8\uB1E2\uB1E3\u0000" + // 15300 - 15309
                "\u0000\uB1E4\u0000\u0000\uB1E5\uB1DD\uB1DE\u0000\uB1DF\u0000" + // 15310 - 15319
                "\uB1E0\u0000\u0000\uFAAF\u0000\uEBFC\u0000\u0000\uE0EA\u0000" + // 15320 - 15329
                "\uF2A2\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uFDBD\uB1DB" + // 15330 - 15339
                "\uB1DC\u0000\u0000\u0000\u0000\u0000\u0000\uF6B0\uEFCF\uE9CF" + // 15340 - 15349
                "\u0000\uEEA4\u0000\u0000\u0000\u0000\uD0A4\u0000\u0000\uE2DE" + // 15350 - 15359
                "\uE1B5\u0000\u0000\uCDEF\uF1A7\uCEE5\uB1D7\uB1D8\u0000\u0000" + // 15360 - 15369
                "\uB1D9\u0000\u0000\uB1DA\uB1D5\u0000\u0000\u0000\uB1D6\u0000" + // 15370 - 15379
                "\u0000\u0000\uE5C0\uFCB5\u0000\u0000\u0000\u0000\uC2E5\u0000" + // 15380 - 15389
                "\u0000\u0000\uF9AC\u0000\u0000\u0000\u0000\uD3DB\uD6B5\uECA4" + // 15390 - 15399
                "\u0000\u0000\uE8C3\u0000\uF1C8\u0000\u0000\u0000\uCEF1\uB1D1" + // 15400 - 15409
                "\uB1D2\u0000\uB1D3\u0000\u0000\u0000\u0000\uD3F7\u0000\u0000" + // 15410 - 15419
                "\u0000\u0000\uC1DC\uC1DD\u0000\uC1DE\uB1D0\u0000\u0000\u0000" + // 15420 - 15429
                "\u0000\u0000\u0000\u0000\uCFCE\uB1CD\uB1CE\u0000\u0000\uB1CF" + // 15430 - 15439
                "\u0000\u0000\u0000\uDBC9\u0000\u0000\u0000\uE4FA\u0000\uD3A5" + // 15440 - 15449
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF7CF\uB1C8\u0000\u0000" + // 15450 - 15459
                "\u0000\u0000\u0000\u0000\u0000\uD7F7\uB1C5\uB1C6\u0000\u0000" + // 15460 - 15469
                "\uB1C7\u0000\u0000\u0000\uD1F4\uD2BA\u0000\u0000\u0000\uDFF2" + // 15470 - 15479
                "\uB1BA\u0000\u0000\uB1BB\uB1BC\uB1BD\uB1BE\u0000\uCFEA\u0000" + // 15480 - 15489
                "\u0000\uCFD0\u0000\uEACC\u0000\u0000\uD7C4\u0000\u0000\u0000" + // 15490 - 15499
                "\u0000\u0000\u0000\uD0A9\u0000\u0000\uDDDA\u0000\u0000\u0000" + // 15500 - 15509
                "\u0000\u0000\u0000\uF5A6\u0000\u0000\uE3A1\u0000\u0000\uE8E3" + // 15510 - 15519
                "\u0000\u0000\uF3AB\uB1B5\u0000\u0000\u0000\u0000\u0000\u0000" + // 15520 - 15529
                "\u0000\uCBBE\uB1B3\u0000\u0000\u0000\uB1B4\u0000\u0000\u0000" + // 15530 - 15539
                "\uEFD8\uE6C9\u0000\uD8B8\uFAF3\u0000\uCDDD\u0000\u0000\u0000" + // 15540 - 15549
                "\u0000\u0000\u0000\u0000\uD9FD\uB1AD\u0000\u0000\u0000\uB1AE" + // 15550 - 15559
                "\u0000\u0000\u0000\uDDD9\u0000\u0000\u0000\u0000\uD9E7\uB1A7" + // 15560 - 15569
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE4D5\uB1A5\u0000" + // 15570 - 15579
                "\u0000\u0000\uB1A6\u0000\u0000\u0000\uEFB8\u0000\u0000\u0000" + // 15580 - 15589
                "\uD7C0\u0000\uECD7\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 15590 - 15599
                "\uEAE6\uB0FC\u0000\u0000\u0000\uB0FD\u0000\uB0FE\u0000\uD0EF" + // 15600 - 15609
                "\u0000\u0000\uFDED\u0000\u0000\u0000\u0000\uC0B8\uC0B9\u0000" + // 15610 - 15619
                "\u0000\uCDDF\u0000\u0000\uF5CB\u0000\uE4F0\uCBAB\uB0F5\uB0F6" + // 15620 - 15629
                "\u0000\uB0F7\u0000\uB0F8\uB0F9\u0000\uF4B7\uFDC2\uFCB0\u0000" + // 15630 - 15639
                "\uFDEC\uCAE2\u0000\u0000\uF6B9\u0000\u0000\u0000\u0000\u0000" + // 15640 - 15649
                "\u0000\uF2B2\uF3F6\uF6DB\uB0F1\u0000\uB0F2\u0000\uB0F3\u0000" + // 15650 - 15659
                "\u0000\uB0F4\uB0ED\uB0EE\u0000\u0000\uB0EF\u0000\u0000\uB0F0" + // 15660 - 15669
                "\uB0E9\u0000\u0000\u0000\uB0EA\u0000\u0000\u0000\uF4AB\u0000" + // 15670 - 15679
                "\u0000\u0000\u0000\uD0BD\uB0E2\uB0E3\u0000\uB0E4\uB0E5\uB0E6" + // 15680 - 15689
                "\u0000\u0000\uDBE7\uE2BF\u0000\u0000\u0000\u0000\u0000\uB5AE" + // 15690 - 15699
                "\u0000\u0000\u0000\uD8F1\u0000\uD4CF\u0000\u0000\u0000\uFAAE" + // 15700 - 15709
                "\u0000\u0000\u0000\uD6E9\uB0E1\u0000\u0000\u0000\u0000\u0000" + // 15710 - 15719
                "\u0000\u0000\uD6DB\uB0DC\uB0DD\uB0DE\u0000\uB0DF\u0000\u0000" + // 15720 - 15729
                "\uB0E0\uB0DA\uB0DB\u0000\u0000\u0000\u0000\u0000\u0000\uA1AD" + // 15730 - 15739
                "\u0000\u0000\u0000\uD3BC\u0000\u0000\u0000\uCAF0\u0000\uCBD3" + // 15740 - 15749
                "\u0000\u0000\u0000\uE0BC\u0000\uF4CA\uD4FA\uB0D5\u0000\u0000" + // 15750 - 15759
                "\u0000\uB0D6\u0000\u0000\u0000\uFACA\u0000\u0000\u0000\uE5E3" + // 15760 - 15769
                "\u0000\uD3D0\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uDFC1" + // 15770 - 15779
                "\uB0CB\uB0CC\u0000\uB0CD\uB0CE\uB0CF\uB0D0\u0000\uF4FA\u0000" + // 15780 - 15789
                "\u0000\u0000\u0000\uCDD6\uFCF6\u0000\uA2D3\uA2D4\u0000\u0000" + // 15790 - 15799
                "\u0000\uA1A5\uA1A6\u0000\u0040\u0041\u0042\u0043\u0044\u0045" + // 15800 - 15809
                "\u0046\u0047\uB0C9\u0000\uB0CA\u0000\u0000\u0000\u0000\u0000" + // 15810 - 15819
                "\uBAEE\uBAEF\u0000\uBAF0\uB0C5\uB0C6\u0000\u0000\uB0C7\u0000" + // 15820 - 15829
                "\u0000\uB0C8\uB0C3\u0000\u0000\u0000\uB0C4\u0000\u0000\u0000" + // 15830 - 15839
                "\uD8B7\uCEB1\uCAC2\u0000\u0000\uFBB4\uB0BF\u0000\u0000\u0000" + // 15840 - 15849
                "\u0000\u0000\u0000\u0000\uEAE0\uB0BC\uB0BD\u0000\u0000\uB0BE" + // 15850 - 15859
                "\u0000\u0000\u0000\uE9BD\u0000\uD7C9\u0000\u0000\uEBDB\uB0BA" + // 15860 - 15869
                "\uB0BB\u0000\u0000\u0000\u0000\u0000\u0000\uA2D0\u0000\uA2D1" + // 15870 - 15879
                "\u0000\uF9EB\uEEA3\u0000\u0000\u0000\u0000\u0000\u0000\uF2D0" + // 15880 - 15889
                "\u0000\uEAFB\uB0B5\u0000\u0000\u0000\uB0B6\u0000\u0000\u0000" + // 15890 - 15899
                "\uE9D7\uE4F1\u0000\u0000\u0000\uCAEF\uB0A8\uB0A9\uB0AA\uB0AB" + // 15900 - 15909
                "\uB0AC\uB0AD\uB0AE\uB0AF\uB0A5\uB0A6\uB0A7\u0000\u0000\u0000" + // 15910 - 15919
                "\u0000\u0000\uBAEA\uBAEB\u0000\u0000\uCDCB\u0000\u0000\u0000" + // 15920 - 15929
                "\u0000\u0000\u0000\uD6DD\u0000\u0000\uE1AB\u0000\u0000\u0000" + // 15930 - 15939
                "\u0000\u0000\u0000\uF3FB\u0000\u0000\uE0FB\u0000\u0000\u0000" + // 15940 - 15949
                "\uEFEA\uFADE\u0000\uE4EE\uF9A1\u0000\u0000\uFBEF\u0000\u0000" + // 15950 - 15959
                "\u0000\uD5B4\u0000\u0000\u0000\u0000\uC5B0\uC5B1\u0000\u0000" + // 15960 - 15969
                "\uD3E0\u0000\uE4BF\u0000\uFBC0\u0000\uDABE\uB0A1\uB0A2\u0000" + // 15970 - 15979
                "\u0000\uB0A3\u0000\u0000\uB0A4\uDBC2\u0000\u0000\u0000\u0000" + // 15980 - 15989
                "\uCAFE\u0000\u0000\uCBB9\u0000\u0000\uEDF9\u0000\u0000\u0000" + // 15990 - 15999
                "\uD9B3\u0000\u0000\uD8F4\u0000\uE9B7\uE0A9\u0000\u0000\u0000" + // 16000 - 16009
                "\u0000\u0000\u0000\u0000\uD7A2\uDCAF\u0000\u0000\u0000\u0000" + // 16010 - 16019
                "\u0000\uF0A3\u0000\uF9E0\u0000\u0000\u0000\u0000\uECD6\u0000" + // 16020 - 16029
                "\u0000\uDFC6\u0000\u0000\u0000\u0000\u0000\u0000\uDFA8\u0000" + // 16030 - 16039
                "\u0000\uF6A6\u0000\u0000\u0000\u0000\u0000\u0000\uCBCA\u0000" + // 16040 - 16049
                "\u0000\uE0DA\u0000\u0000\u0000\uEEF7\u0000\u0000\uDFA3\u0000" + // 16050 - 16059
                "\u0000\u0000\u0000\u0000\u0000\uECB7\u0000\u0000\uFDDF\u0000" + // 16060 - 16069
                "\u0000\u0000\u0000\u0000\u0000\uE5CB\u0000\u0000\uCBAA\u0000" + // 16070 - 16079
                "\u0000\u0000\u0000\u0000\u0000\uDCAD\u0000\u0000\uE4DB\u0000" + // 16080 - 16089
                "\uE1FB\uCBA2\u0000\u0000\u0000\uECBC\u0000\u0000\uE5AD\u0000" + // 16090 - 16099
                "\uA1AE\uA1AF\u0000\u0000\uA1B0\uA1B1\u0000\u0000\uE7D5\uF5BF" + // 16100 - 16109
                "\uCFA2\uCDAF\uCFA3\u0000\u0000\uCDB0\uF1FE\uD0A3\uE1AF\uF8A3" + // 16110 - 16119
                "\u0000\uCAA6\uD3DA\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 16120 - 16129
                "\uF1E7\uD9F9\u0000\u0000\uD3EA\uF5F5\u0000\uEFC7\u0000\uEDAA" + // 16130 - 16139
                "\u0000\u0000\uF2A1\uCEE1\u0000\u0000\u0000\uFDB8\uE3E8\u0000" + // 16140 - 16149
                "\uD4A7\uE8FC\uFDD3\uEBED\uD6DC\u0000\u0000\u0000\u0000\u0000" + // 16150 - 16159
                "\uBADC\u0000\u0000\u0000\uFCE1\uEDB0\uFDD1\uF6BB\u0000\u0000" + // 16160 - 16169
                "\uFBB6\u0000\u0000\u0000\u0000\u0000\u0000\uE4BD\u0000\u0000" + // 16170 - 16179
                "\uE1C5\u0000\u0000\u0000\u0000\u0000\u0000\uFDA3\u0000\uFBE5" + // 16180 - 16189
                "\uCDDC\uD9F7\u0000\u0000\u0000\u0000\u0000\u0000\uA1E5\uA1E4" + // 16190 - 16199
                "\u0000\u0000\uF5AF\u0000\u0000\u0000\u0000\uCEF0\u0000\uDCFB" + // 16200 - 16209
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF8B5\uE4E5\u0000" + // 16210 - 16219
                "\u0000\u0000\u0000\u0000\u0000\u0000\uD9F6\uE4CA\u0000\uDCE1" + // 16220 - 16229
                "\u0000\u0000\uF9C8\u0000\u0000\uF3F9\u0000\uEDF8\u0000\uF5C7" + // 16230 - 16239
                "\u0000\u0000\uF1D3\uF5E7\u0000\u0000\u0000\u0000\uCADA\uCCF4" + // 16240 - 16249
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE4E1\uCDD4\u0000" + // 16250 - 16259
                "\u0000\u0000\u0000\u0000\u0000\u0000\uEDFE\uFAB5\u0000\u0000" + // 16260 - 16269
                "\u0000\u0000\u0000\u0000\u0000\uCCC6\uCDE9\u0000\u0000\u0000" + // 16270 - 16279
                "\u0000\u0000\u0000\u0000\uE2F5\uD8E9\u0000\u0000\uF8FE\u0000" + // 16280 - 16289
                "\uCFCC\u0000\u0000\uE9C8\u0000\uCBCF\u0000\uE3C9\u0000\u0000" + // 16290 - 16299
                "\uF3D7\u0000\u0000\u0000\uDCBD\u0000\uCCE5\uD4F9\u0000\u0000" + // 16300 - 16309
                "\u0000\u0000\u0000\uF5E2\uE1D3\uDCC0\u0000\u0000\u0000\u0000" + // 16310 - 16319
                "\u0000\uD1C8\uD1C9\uF1D2\uD2CC\uCFCB\u0000\u0000\uCABD\u0000" + // 16320 - 16329
                "\u0000\uF8E3\u0000\uD4DD\u0000\u0000\uEAD8\u0000\uEEBA\u0000" + // 16330 - 16339
                "\u0000\u0000\u0000\u0000\uF8D3\u0000\uA1A9\u0000\u0000\u0000" + // 16340 - 16349
                "\uA1AA\uA1AA\u0000\u0000\uF9A3\uE0DB\uF6EB\u0000\uCBF1\uFBB0" + // 16350 - 16359
                "\u0000\u0000\u0000\uD8A9\uE5DF\uF9A7\u0000\uF1B2\u0000\uF1B1" + // 16360 - 16369
                "\u0000\u0000\u0000\u0000\u0000\uF2BC\uECE3\u0000\u0000\uE1B4" + // 16370 - 16379
                "\u0000\u0000\u0000\u0000\uF4D3\u0000\uACEA\uACEB\uACEC\uACED" + // 16380 - 16389
                "\uACEE\uACEF\uACF0\uACF1\uCEBD\u0000\u0000\u0000\u0000\u0000" + // 16390 - 16399
                "\u0000\u0000\uD4F8\uDCBF\u0000\u0000\u0000\u0000\u0000\u0000" + // 16400 - 16409
                "\u0000\uE6E3\uE6AE\u0000\u0000\u0000\u0000\u0000\uEFB6\u0000" + // 16410 - 16419
                "\uEFD0\u0000\uCDB1\u0000\u0000\u0000\u0000\u0000\uDFD8\u0000" + // 16420 - 16429
                "\u0000\u0000\uDCB9\u0000\u0000\u0000\uF1C0\uF3C9\u0000\u0000" + // 16430 - 16439
                "\uE4BB\u0000\u0000\u0000\u0000\uDECD\uECF3\u0000\u0000\uEDE0" + // 16440 - 16449
                "\uF9A6\u0000\u0000\u0000\u0000\u0000\u0000\uDFBD\uEAC3\u0000" + // 16450 - 16459
                "\uEFB4\u0000\u0000\u0000\uD7BE\u0000\uFDE5\uF6A3\u0000\uD9FC" + // 16460 - 16469
                "\uFDA9\u0000\uE7EE\u0000\uACE2\uACE3\uACE4\uACE5\uACE6\uACE7" + // 16470 - 16479
                "\uACE8\uACE9\uCCF2\uF7DD\u0000\uDEBA\u0000\u0000\u0000\u0000" + // 16480 - 16489
                "\uD9D7\u0000\u0000\u0000\u0000\uC1A6\uC1A7\u0000\u0000\uF4EA" + // 16490 - 16499
                "\u0000\u0000\u0000\uCEB9\u0000\u0000\uF6E0\u0000\u0000\u0000" + // 16500 - 16509
                "\u0000\uE9F3\uF2C3\uFAFA\u0000\u0000\u0000\u0000\u0000\u0000" + // 16510 - 16519
                "\u0000\uCDAC\uD6C5\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 16520 - 16529
                "\uCDDB\uE7E8\uE8D7\uDAF8\uD4CB\u0000\u0000\u0000\uF7F6\uE2CE" + // 16530 - 16539
                "\u0000\uE9F5\u0000\uE1EB\u0000\u0000\u0000\uF1B7\uEEF8\u0000" + // 16540 - 16549
                "\u0000\u0000\uD9D9\uFBE1\uFAED\uF0A2\uCCF1\u0000\uFAA3\uE2F7" + // 16550 - 16559
                "\u0000\uF8C5\u0000\u0000\u0000\u0000\u0000\uDCFA\u0000\uACDA" + // 16560 - 16569
                "\uACDB\uACDC\uACDD\uACDE\uACDF\uACE0\uACE1\uCAEA\u0000\u0000" + // 16570 - 16579
                "\uCFD4\u0000\uF8BD\u0000\u0000\uDDB4\uE4B5\uD8B0\u0000\u0000" + // 16580 - 16589
                "\u0000\u0000\uC8D1\uC8D2\u0000\u0000\uF2C2\uF6C3\u0000\uD7D2" + // 16590 - 16599
                "\u0000\u0000\uF9A2\uCFD3\u0000\u0000\u0000\u0000\u0000\u0000" + // 16600 - 16609
                "\u0000\uE0A4\uD6C4\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 16610 - 16619
                "\uD0DC\uDFAC\u0000\uD6DA\u0000\u0000\u0000\u0000\u0000\uB9C2" + // 16620 - 16629
                "\u0000\u0000\u0000\uCCD2\u0000\uDDA4\u0000\u0000\u0000\uEAD6" + // 16630 - 16639
                "\uF1B0\u0000\u0000\u0000\uE2EE\u0000\u0000\u0000\u0000\uC7F0" + // 16640 - 16649
                "\uC7F1\u0000\uC7F2\uE2CD\u0000\u0000\u0000\u0000\u0000\uEFFD" + // 16650 - 16659
                "\uF2E8\uDDD4\u0000\uEAA3\u0000\u0000\u0000\uD6C3\uD6F4\uE9EB" + // 16660 - 16669
                "\uE9EC\uE0E4\u0000\u0000\u0000\u0000\uDAA7\uEDCD\uE4D2\u0000" + // 16670 - 16679
                "\u0000\uEAA9\uE4BA\uF3A2\uCDD2\uE2CB\u0000\uFACF\u0000\u0000" + // 16680 - 16689
                "\u0000\u0000\u0000\uB9BE\u0000\u0000\u0000\uCACE\uF8C1\uD2B4" + // 16690 - 16699
                "\u0000\u0000\uDCB4\uE4F6\uD0C0\u0000\uF0B7\uEEA1\u0000\u0000" + // 16700 - 16709
                "\u0000\uE3BC\uF8D6\u0000\u0000\uDBEE\u0000\uF7CE\uFABE\u0000" + // 16710 - 16719
                "\u0000\u0000\u0000\u0000\u0000\uFCCE\u0000\uDDEE\uD7C1\u0000" + // 16720 - 16729
                "\u0000\u0000\u0000\uE5D5\u0000\u0000\uF5AE\uFBAA\u0000\u0000" + // 16730 - 16739
                "\u0000\u0000\uECFB\uEBE4\u0000\u0000\uF2E7\u0000\uD7D5\uD4B6" + // 16740 - 16749
                "\uF9E8\uF9DA\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uDCDC" + // 16750 - 16759
                "\uF6ED\u0000\uF9AE\u0000\uDDBE\u0000\u0000\u0000\uE5F5\u0000" + // 16760 - 16769
                "\u0000\u0000\u0000\u0000\uE9CA\u0000\uE1F0\u0000\uF9EA\uD1CE" + // 16770 - 16779
                "\uEED4\u0000\uD4D2\uD9A3\uFDA8\uD7D9\uD0B1\u0000\u0000\u0000" + // 16780 - 16789
                "\u0000\uD5EF\u0000\u0000\uD8C9\u0000\u0000\u0000\u0000\u0000" + // 16790 - 16799
                "\u0000\uFDA4\u0000\u0000\uCEA1\uF5A9\u0000\u0000\uDDF9\u0000" + // 16800 - 16809
                "\u0000\uE0FA\uEEC4\uD9DE\u0000\u0000\u0000\u0000\uC5B4\uC5B5" + // 16810 - 16819
                "\u0000\uC5B6\uCBA6\u0000\u0000\u0000\u0000\u0000\u0000\u0000" + // 16820 - 16829
                "\uD7EA\uDAA6\u0000\u0000\uE0EC\u0000\u0000\u0000\u0000\uFAC6" + // 16830 - 16839
                "\u0000\u0000\u0000\u0000\uC0AB\uC0AC\u0000\uC0AD\uF7A1\u0000" + // 16840 - 16849
                "\u0000\u0000\u0000\u0000\u0000\u0000\uD5C5\uF1A4\u0000\u0000" + // 16850 - 16859
                "\u0000\u0000\u0000\u0000\u0000\uD9AD\uECF0\u0000\u0000\u0000" + // 16860 - 16869
                "\u0000\u0000\u0000\u0000\uEADA\uE7CC\u0000\uD6A8\uCEA7\u0000" + // 16870 - 16879
                "\uD4B5\u0000\u0000\uECBE\u0000\u0000\u0000\uE5B4\uCDC8\uEEC8" + // 16880 - 16889
                "\uD9E1\u0000\u0000\uE0B8\u0000\u0000\uCDD1\uF3B9\uEFFC\uD1C4" + // 16890 - 16899
                "\uEFB1\u0000\uD1C5\u0000\uD0DE\u0000\uDEC9\u0000\u0000\u0000" + // 16900 - 16909
                "\u0000\u0000\u0000\u0000\uDBD7\uF5DF\u0000\uEEB6\u0000\u0000" + // 16910 - 16919
                "\u0000\uE2F6\uD3CA\uF5DE\u0000\u0000\u0000\u0000\u0000\u0000" + // 16920 - 16929
                "\u0000\uDEEF\uCBEA\u0000\u0000\u0000\uCBBC\u0000\u0000\u0000" + // 16930 - 16939
                "\uFBE6\u0000\u0000\u0000\u0000\u0000\uE0A7\u0000\u0000\u0000" + // 16940 - 16949
                "\uD4AB\uCAB3\uCDA6\u0000\uCDC3\uD9AF\u0000\u0000\u0000\uF9E7" + // 16950 - 16959
                "\u0000\u0000\u0000\uECF4\u0000\u0000\u0000\u0000\u0000\uEFFB" + // 16960 - 16969
                "\u0000\u0000\uFAF9\uEBDE\u0000\u0000\uF5C8\u0000\uD4DE\u0000" + // 16970 - 16979
                "\u0000\uFCA3\u0000\uDBBB\u0000\u0000\u0000\uF2BA\uCBA5\u0000" + // 16980 - 16989
                "\u0000\u0000\u0000\uCBE9\u0000\u0000\uDDB3\uD4EC\u0000\u0000" + // 16990 - 16999
                "\uF2B9\u0000\uDFB7\uCCA2\uF7FE\uDFBC\u0000\u0000\u0000\u0000" + // 17000 - 17009
                "\uEBCD\uEFF9\u0000\u0000\u0000\uDDBC\uF6DC\u0000\u0000\uCADE" + // 17010 - 17019
                "\uDFE4\u0000\u0000\u0000\uE6FD\u0000\uF7BB\uF2EA\uDEC8\uE9D3" + // 17020 - 17029
                "\u0000\u0000\u0000\u0000\uBFE4\uBFE5\u0000\u0000\uDCE6\u0000" + // 17030 - 17039
                "\u0000\uDED2\u0000\u0000\uEDE2\uD7ED\uD1D1\u0000\u0000\u0000" + // 17040 - 17049
                "\u0000\u0000\uE1F2\uE5D4\u0000\u0000\u0000\u0000\u0000\u0000" + // 17050 - 17059
                "\uF3FA\uDFAB\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uFDD0" + // 17060 - 17069
                "\uDDBB\u0000\u0000\u0000\u0000\uCEAC\u0000\u0000\uF3EC\u0000" + // 17070 - 17079
                "\u0000\u0000\u0000\uDEA1\u0000\uE9D1\uF3A9\uD0E0\uE9D2\u0000" + // 17080 - 17089
                "\uDAE3\u0000\u0000\uCDFB\uF6D6\u0000\uE7F5\uE8EF\uE3F9\uD2BB" + // 17090 - 17099
                "\uEED0\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD5A1\uF7C6" + // 17100 - 17109
                "\uCFC8\u0000\u0000\u0000\uE1D0\u0000\u0000\uCECD\u0000\u0000" + // 17110 - 17119
                "\uD4DC\u0000\u0000\u0000\uDCB1\u0000\u0000\u0000\uD5D6\u0000" + // 17120 - 17129
                "\uF6B2\u0000\u0000\u0000\u0000\uCFF0\uF9BD\u0000\uACD1\uACD2" + // 17130 - 17139
                "\uACD3\uACD4\uACD5\uACD6\uACD8\uACD9\uF5BE\u0000\uDEF7\u0000" + // 17140 - 17149
                "\u0000\u0000\u0000\uCAFB\uD8B1\u0000\uDCAB\u0000\u0000\u0000" + // 17150 - 17159
                "\u0000\uD5A4\uE9AD\uD8E4\uFAB3\uE2C5\uFCBD\u0000\u0000\uECC4" + // 17160 - 17169
                "\uE0D4\u0000\uEBB6\u0000\uD7A1\uCBE8\u0000\uF9AD\uEAC0\uE1CF" + // 17170 - 17179
                "\u0000\uCCBA\u0000\u0000\u0000\u0000\uE3E2\uFBBC\uD9A4\u0000" + // 17180 - 17189
                "\u0000\uCEDD\uEBC0\u0000\uFDA2\u0000\u0000\u0000\uD7D6\u0000" + // 17190 - 17199
                "\u0000\uDCC1\u0000\uDEC6\uF9C6\uFCDA\u0000\uD4B3\uD3B9\uEADE" + // 17200 - 17209
                "\u0000\u0000\uCCB7\uDBB8\u0000\u0000\u0000\u0000\uD0E9\uECEF" + // 17210 - 17219
                "\u0000\u0000\u0000\uF9BA\u0000\uEBB5\u0000\uD7D8\u0000\uFDA7" + // 17220 - 17229
                "\u0000\u0000\u0000\u0000\uEAAB\uF0E3\uF1E4\uDCF1\uD6A7\u0000" + // 17230 - 17239
                "\u0000\u0000\u0000\uE7EF\u0000\u0000\u0000\u0000\uBFF6\uBFF7" + // 17240 - 17249
                "\u0000\u0000\uEBBF\u0000\uD7CE\uD1BF\u0000\u0000\u0000\uCCF5" + // 17250 - 17259
                "\uF5B5\uE4AD\u0000\u0000\u0000\uDBF2\u0000\u0000\u0000\u0000" + // 17260 - 17269
                "\uC4D4\uC4D5\u0000\uC4D6\uF5EF\uCFC7\u0000\u0000\uD4B2\uCCEF" + // 17270 - 17279
                "\u0000\uD4E8\uFBAD\u0000\u0000\uF8E7\u0000\uE1CE\u0000\uF7E2" + // 17280 - 17289
                "\uF7DC\uE1EA\uCEC1\uD4B1\u0000\uFDB1\uE6BD\u0000\uECD4\uEACB" + // 17290 - 17299
                "\u0000\u0000\uCABF\uD5B0\u0000\uCFE9\uE2FB\u0000\uCCA6\u0000" + // 17300 - 17309
                "\u0000\u0000\u0000\uDABB\uF2E3\uE9B4\uD2DC\u0000\u0000\u0000" + // 17310 - 17319
                "\u0000\u0000\uB7D2\uB7D3\u0000\uB7D4\uDCA9\u0000\u0000\u0000" + // 17320 - 17329
                "\u0000\uDEF6\u0000\uDCAA\uE2C3\uDCDE\u0000\uDCDF\u0000\u0000" + // 17330 - 17339
                "\uEFAD\uE6AB\uF5EE\u0000\u0000\uCABB\u0000\u0000\uE3DC\u0000" + // 17340 - 17349
                "\uE0D9\u0000\u0000\u0000\u0000\u0000\u0000\uD9D6\uCFC6\u0000" + // 17350 - 17359
                "\u0000\u0000\u0000\u0000\u0000\u0000\uEECB\uF4B2\u0000\u0000" + // 17360 - 17369
                "\u0000\u0000\u0000\u0000\u0000\uEBFB\uD4B0\uF3B2\uFBB7\u0000" + // 17370 - 17379
                "\u0000\u0000\u0000\u0000\uB7C1\uB7C2\u0000\u0000\uF8BB\u0000" + // 17380 - 17389
                "\uE8D1\u0000\u0000\u0000\u0000\uC7DC\uC7DD\u0000\uC7DE\uEBB2" + // 17390 - 17399
                "\u0000\u0000\u0000\u0000\uF1A2\u0000\u0000\uE8BA\u0000\u0000" + // 17400 - 17409
                "\u0000\uE3C7\u0000\u0000\uFADF\u0000\u0000\u0000\u0000\u0000" + // 17410 - 17419
                "\u0000\uF1CA\u0000\uCEA3\uF4C2\u0000\u0000\u0000\u0000\u0000" + // 17420 - 17429
                "\u0000\u0000\uEECA\uCEA5\u0000\u0000\u0000\u0000\u0000\u0000" + // 17430 - 17439
                "\uD6D8\uF5D9\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD9AB" + // 17440 - 17449
                "\uF1CB\u0000\u0000\uD0AF\uDDB9\u0000\u0000\uD1C3\uF1FC\u0000" + // 17450 - 17459
                "\uF3C7\u0000\u0000\uE0EB\u0000\u0000\uE6BA\u0000\u0000\uCDA9" + // 17460 - 17469
                "\u0000\u0000\u0000\uE7D1\uD2AC\u0000\uCEF9\u0000\u0000\uD4FD" + // 17470 - 17479
                "\u0000\u0000\uE0C8\u0000\u0000\u0000\uB6BB\uB6BC\uB6BD\u0000" + // 17480 - 17489
                "\u0000\uD9ED\u0000\u0000\u0000\u0000\uF5A5\u0000\uEFBF\u0000" + // 17490 - 17499
                "\u0000\u0000\u0000\u0000\uCECF\u0000\uACBA\uACBB\uACBC\uACBD" + // 17500 - 17509
                "\uACBE\uACBF\uACC0\uACC1\uDBD3\u0000\uFAE7\uD8E3\uF4C1\u0000" + // 17510 - 17519
                "\uDDB7\u0000\uE7ED\uFDC1\uDAE2\u0000\u0000\uD8B3\u0000\u0000" + // 17520 - 17529
                "\uFAA9\u0000\uE1DD\u0000\u0000\u0000\u0000\uC1CE\uC1CF\u0000" + // 17530 - 17539
                "\uC1D0\uCAB9\u0000\uEEE4\u0000\u0000\u0000\u0000\u0000\uB7AA" + // 17540 - 17549
                "\uB7AB\u0000\u0000\uE3B1\uFCEB\uCDA8\u0000\uCCB6\u0000\u0000" + // 17550 - 17559
                "\uF6C2\u0000\u0000\uEFBB\u0000\u0000\u0000\uB6A6\uB6A7\uB6A8" + // 17560 - 17569
                "\u0000\u0000\uDBCC\uDDCD\u0000\u0000\u0000\uD4C8\u0000\uCFA1" + // 17570 - 17579
                "\uE4A8\u0000\uF4B6\uECFE\u0000\u0000\uE3AE\uF9C5\uDDD3\uD6F1" + // 17580 - 17589
                "\uECFC\uFCF0\u0000\u0000\uEDC0\uD3E8\u0000\u0000\uDEA8\uF4E4" + // 17590 - 17599
                "\uECC2\u0000\uD9F5\uE1AE\u0000\u0000\uECC3\uCFFE\u0000\uF8BF" + // 17600 - 17609
                "\uD8E2\uFCA7\uF7FC\uF7B1\uCEBB\uF4A1\u0000\u0000\uEECD\uDDB6" + // 17610 - 17619
                "\uEEAF\uCDF8\u0000\u0000\u0000\u0000\uDEB8\uD1C2\u0000\uF9A5" + // 17620 - 17629
                "\u0000\uE8D5\u0000\u0000\u0000\uCCF7\u0000\u0000\u0000\u0000" + // 17630 - 17639
                "\u0000\uF5DD\u0000\u0000\u0000\uEAD4\u0000\u0000\u0000\u0000" + // 17640 - 17649
                "\uC5E1\u0000\u0000\u0000\uDFA7\u0000\uDFE7\uE1C1\u0000\uEDDD" + // 17650 - 17659
                "\uCEC4\u0000\uCBA1\u0000\u0000\u0000\u0000\uBFA9\uBFAA\uBFAB" + // 17660 - 17669
                "\u0000\uACB2\uACB3\uACB4\uACB5\uACB6\uACB7\uACB8\uACB9\uE2C2" + // 17670 - 17679
                "\u0000\uF3D8\uE5D3\u0000\u0000\uF3D9\u0000\uDCD3\u0000\u0000" + // 17680 - 17689
                "\u0000\u0000\uDDE2\uFBF9\uDDC1\uD4C1\u0000\u0000\u0000\u0000" + // 17690 - 17699
                "\u0000\u0000\u0000\uD6A4\uECA1\u0000\u0000\u0000\uCCB9\u0000" + // 17700 - 17709
                "\u0000\uFBDE\uE3DB\u0000\uD3C9\u0000\uDCCF\u0000\u0000\u0000" + // 17710 - 17719
                "\uF8C0\u0000\u0000\u0000\uD3DD\u0000\uFDD6\u0000\u0000\u0000" + // 17720 - 17729
                "\u0000\uF8D1\u0000\uF8D2\uD9C8\u0000\u0000\uEEE3\uD7BD\u0000" + // 17730 - 17739
                "\u0000\u0000\uDBA9\u0000\u0000\uD3BB\uCAEC\u0000\uCFE8\u0000" + // 17740 - 17749
                "\uEDC3\uD0B2\u0000\u0000\uCEFE\uDAA8\uE9AA\u0000\u0000\u0000" + // 17750 - 17759
                "\u0000\u0000\u0000\u0000\uEEE1\uDACD\u0000\u0000\u0000\uF9CC" + // 17760 - 17769
                "\u0000\uE1DA\uDBBF\uD9C7\uE4D7\uEADD\u0000\uD4F7\u0000\u0000" + // 17770 - 17779
                "\u0000\uEAAE\uEAAD\u0000\u0000\uD3F1\u0000\uCAEB\uD9E2\u0000" + // 17780 - 17789
                "\uFDB2\u0000\uE3AD\uD6CC\uD9B4\uF0B3\u0000\uE5EC\u0000\u0000" + // 17790 - 17799
                "\u0000\uD1E7\u0000\uCAA5\u0000\u0000\uD6AB\uD0C2\u0000\u0000" + // 17800 - 17809
                "\u0000\uD8BD\u0000\u0000\u0000\u0000\uC5A1\uC5A2\u0000\uC5A3" + // 17810 - 17819
                "\uE2C1\u0000\uCEA4\u0000\u0000\u0000\u0000\u0000\uB6D1\uB6D2" + // 17820 - 17829
                "\u0000\u0000\uF2DB\uE4FC\u0000\u0000\u0000\u0000\u0000\uB3A6" + // 17830 - 17839
                "\uB3A7\u0000\uB3A8\uFCEF\u0000\uE0E3\u0000\u0000\u0000\u0000" + // 17840 - 17849
                "\u0000\uB6CF\u0000\u0000\u0000\uE1F6\uDECC\u0000\u0000\uFCDE" + // 17850 - 17859
                "\u0000\uDBF9\uD7B1\u0000\u0000\u0000\uCBFC\u0000\u0000\uFDF0" + // 17860 - 17869
                "\u0000\uE0BD\uCEE3\u0000\u0000\u0000\uEDB9\uF1C5\u0000\uF3CF" + // 17870 - 17879
                "\uD7AB\uE1A4\uCDAB\u0000\uD9F4\uE8A6\uCDCE\uE1E9\u0000\uCFE7" + // 17880 - 17889
                "\uF3CB\uEDA9\uCABE\u0000\u0000\u0000\u0000\uBEEE\uBEEF\u0000" + // 17890 - 17899
                "\u0000\uD2EC\u0000\u0000\u0000\u0000\u0000\u0000\uF9C2\u0000" + // 17900 - 17909
                "\uEABC\uEBAF\u0000\u0000\u0000\u0000\u0000\uE5DE\u0000\uD3F0" + // 17910 - 17919
                "\u0000\u0000\u0000\u0000\u0000\uF0A4\uE1EC\uFDFE\uFCA5\uFAB1" + // 17920 - 17929
                "\uDFD9\u0000\uE0D2\u0000\u0000\uCCB5\u0000\u0000\u0000\u0000" + // 17930 - 17939
                "\u0000\uCFBD\uE7C9\u0000\uE2F3\uE7E1\u0000\u0000\uE3CB\u0000" + // 17940 - 17949
                "\uE7D4\u0000\uCACA\u0000\u0000\u0000\uD9FB\u0000\uACAA\uACAB" + // 17950 - 17959
                "\uACAC\uACAD\uACAE\uACAF\uACB0\uACB1\uDEF1\u0000\u0000\u0000" + // 17960 - 17969
                "\uF0DF\uF8C4\u0000\u0000\uF7D6\uDEEA\uCBB4\u0000\u0000\uEFBE" + // 17970 - 17979
                "\u0000\uD3EF\u0000\u0000\uECD3\u0000\u0000\uDDC2\uEFB7\uD1C0" + // 17980 - 17989
                "\u0000\u0000\uE8C5\u0000\uE4B8\u0000\uE1E8\uCDAA\u0000\uE3F2" + // 17990 - 17999
                "\u0000\uFBF7\u0000\uF7D0\u0000\uF4C8\uE8EA\uF5F3\u0000\u0000" + // 18000 - 18009
                "\uF9DE\u0000\u0000\uEED5\u0000\u0000\u0000\u0000\uF9F4\u0000" + // 18010 - 18019
                "\uACA1\uACA2\uACA3\uACA4\uACA5\uACA6\uACA8\uACA9\uE5EB\u0000" + // 18020 - 18029
                "\uEFF4\uDDB5\u0000\u0000\u0000\u0000\uE5E7\u0000\u0000\u0000" + // 18030 - 18039
                "\uCAA3\uF5BA\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF6C5" + // 18040 - 18049
                "\uCEBA\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF0EA\uD7AE" + // 18050 - 18059
                "\u0000\u0000\uE0E1\u0000\u0000\u0000\u0000\uEEEC\u0000\u0000" + // 18060 - 18069
                "\uD3A3\uEEB7\uEADC\uDBD2\u0000\u0000\u0000\u0000\u0000\u0000" + // 18070 - 18079
                "\uD7A3\u0000\u0000\uE1C9\uCAFA\u0000\u0000\u0000\u0000\u0000" + // 18080 - 18089
                "\uB2F4\uB2F5\u0000\u0000\uEFA5\u0000\uD3CC\uDAED\u0000\u0000" + // 18090 - 18099
                "\u0000\uB2FA\uB2FB\uB2FC\u0000\uB2FD\uCAE8\u0000\uF8E6\uDCCE" + // 18100 - 18109
                "\u0000\u0000\u0000\u0000\uECF2\u0000\u0000\u0000\u0000\uBEDF" + // 18110 - 18119
                "\uBEE0\u0000\u0000\uE2B6\u0000\u0000\u0000\u0000\uEFF1\u0000" + // 18120 - 18129
                "\uFCC5\uCBC2\u0000\u0000\u0000\u0000\uFDD5\u0000\uA5F7\uA5F8" + // 18130 - 18139
                "\u0000\u0000\u0000\u0000\u0000\u0000\uD8AD\u0000\u0000\uDDE4" + // 18140 - 18149
                "\uF0EF\uF6F1\uFAF0\u0000\u0000\uD1F5\uD5A3\u0000\u0000\u0000" + // 18150 - 18159
                "\u0000\uF0B2\u0000\u0000\uD5DC\uF3C4\uCBD7\u0000\u0000\u0000" + // 18160 - 18169
                "\u0000\uC7B6\u0000\u0000\u0000\uA1E3\uA1E2\u0000\u0000\uA2BA" + // 18170 - 18179
                "\uA2B9\uDEA5\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uCBCD" + // 18180 - 18189
                "\uCAB7\u0000\u0000\uD3E7\u0000\uF8E5\u0000\u0000\uCADB\u0000" + // 18190 - 18199
                "\u0000\u0000\u0000\u0000\uFCD7\uE1F1\u0000\uD2A4\u0000\u0000" + // 18200 - 18209
                "\u0000\u0000\uF5FB\uF8FA\u0000\u0000\uDFB9\u0000\u0000\u0000" + // 18210 - 18219
                "\u0000\uEAE1\u0000\u0000\uDCE3\uDFAD\uFAEC\u0000\u0000\u0000" + // 18220 - 18229
                "\u0000\u0000\uF1EB\u0000\uCEAE\u0000\u0000\u0000\u0000\uD9A2" + // 18230 - 18239
                "\u0000\u0000\uF2EC\u0000\u0000\uFAEE\u0000\u0000\u0000\uDAB7" + // 18240 - 18249
                "\u0000\u0000\u0000\u0000\uC7A5\u0000\u0000\u0000\uDDFB\u0000" + // 18250 - 18259
                "\u0000\u0000\u0000\uC7B4\u0000\u0000\u0000\uCDA7\u0000\u0000" + // 18260 - 18269
                "\uD0AC\u0000\uA5F0\uA5F1\u0000\uA5F2\uA5F3\uA5F4\uA5F5\uA5F6" + // 18270 - 18279
                "\uDAF6\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uFBEE\uE8C4" + // 18280 - 18289
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD9DF\uE3A4\u0000" + // 18290 - 18299
                "\u0000\u0000\u0000\u0000\u0000\u0000\uE2B4\uF2BB\u0000\uDEA4" + // 18300 - 18309
                "\u0000\uDACC\u0000\u0000\u0000\uE1A7\uEED3\uD0C3\u0000\u0000" + // 18310 - 18319
                "\u0000\uFBFD\u0000\u0000\u0000\u0000\uC5A9\uC5AA\u0000\u0000" + // 18320 - 18329
                "\uDFD5\u0000\u0000\uEDD7\u0000\u0000\u0000\uA1D6\u0000\u0000" + // 18330 - 18339
                "\u0000\u0000\u0000\uE1CA\uEBE3\u0000\uF2DE\uE8A5\u0000\u0000" + // 18340 - 18349
                "\u0000\u0000\u0000\u0000\u0000\uE6DA\uF5B9\u0000\uDCF0\uE3F1" + // 18350 - 18359
                "\u0000\u0000\u0000\u0000\uA8F7\uA8F8\u0000\u0000\u0000\uD7CD" + // 18360 - 18369
                "\u0000\u0000\uD4D1\uE9ED\u0000\uD0ED\u0000\u0000\u0000\u0000" + // 18370 - 18379
                "\u0000\uE5F7\u0000\uA5E8\uA5E9\uA5EA\uA5EB\uA5EC\uA5ED\uA5EE" + // 18380 - 18389
                "\uA5EF\uF9CB\u0000\u0000\u0000\uCBF3\uF4A5\u0000\u0000\uD4F5" + // 18390 - 18399
                "\u0000\uD0C9\uEFA7\uE2EC\u0000\uDBEA\uFDB9\u0000\u0000\u0000" + // 18400 - 18409
                "\u0000\u0000\u0000\u0000\uF5B8\uFDBC\uDFB1\uE3EF\u0000\u0000" + // 18410 - 18419
                "\u0000\u0000\uE0A3\uCFF2\uF7B9\uD9F3\u0000\u0000\uE1CB\u0000" + // 18420 - 18429
                "\u0000\uDFE9\u0000\uEEDE\u0000\u0000\uF7C2\u0000\uEEF0\u0000" + // 18430 - 18439
                "\u0000\u0000\uCCC2\u0000\u0000\u0000\uD8BC\uF2B0\u0000\u0000" + // 18440 - 18449
                "\u0000\uF9D4\uF7CA\u0000\u0000\uD6C8\uDFB8\u0000\uEAA5\u0000" + // 18450 - 18459
                "\u0000\u0000\uD7AD\u0000\uEEEF\uD5D7\uEAE4\uF8A2\uCDEB\uD7BF" + // 18460 - 18469
                "\uFBB1\u0000\uA5D7\uA5D8\u0000\u0000\u0000\u0000\u0000\u0000" + // 18470 - 18479
                "\uE1FD\u0000\u0000\uD8F5\u0000\u0000\u0000\uCCCE\u0000\u0000" + // 18480 - 18489
                "\uE3B5\u0000\u0000\u0000\u0000\u0000\u0000\uE9BE\u0000\u0000" + // 18490 - 18499
                "\uF9AF\u0000\u0000\u0000\u0000\u0000\uD2FB\uE1E0\u0000\uD9AC" + // 18500 - 18509
                "\u0000\uF5EB\u0000\uE0B6\u0000\uE0BB\uCEC3\u0000\uD0BA\uF7BA" + // 18510 - 18519
                "\uD8F3\uF7CD\u0000\uA5D0\uA5D1\u0000\uA5D2\uA5D3\uA5D4\uA5D5" + // 18520 - 18529
                "\uA5D6\uF1FA\u0000\u0000\uE5B6\uF3EF\u0000\u0000\uFBDA\uE2BD" + // 18530 - 18539
                "\u0000\u0000\u0000\uE3C8\u0000\u0000\u0000\uE4AE\u0000\u0000" + // 18540 - 18549
                "\u0000\u0000\u0000\uCDCD\u0000\u0000\u0000\uDBCB\uDAB5\u0000" + // 18550 - 18559
                "\u0000\u0000\uF5D3\u0000\u0000\uECDC\uF7B7\uEBAD\u0000\u0000" + // 18560 - 18569
                "\u0000\u0000\uD5AA\u0000\u0000\uCFE2\uCDF6\u0000\u0000\uEFF0" + // 18570 - 18579
                "\u0000\uF4BE\uCDCA\uD7D4\uDEA3\u0000\uE4E0\u0000\u0000\u0000" + // 18580 - 18589
                "\uEEB9\u0000\u0000\u0000\u0000\uD5E3\uE3EE\u0000\u0000\u0000" + // 18590 - 18599
                "\u0000\u0000\uE8D4\u0000\uE8B4\uEBC3\u0000\uEAAA\uFAFC\uF5F6" + // 18600 - 18609
                "\uF0BC\uFDD4\uE2F0\u0000\u0000\u0000\u0000\u0000\u0000\uFABB" + // 18610 - 18619
                "\uE9C7\uE6AA\u0000\u0000\u0000\u0000\u0000\u0000\uCEF6\u0000" + // 18620 - 18629
                "\uFAD0\uEDBC\u0000\u0000\uD8D4\u0000\u0000\u0000\uDCDA\uE9FD" + // 18630 - 18639
                "\uD0CA\u0000\uF5D6\uD9C5\uE4B4\u0000\uEDA7\uF5AC\u0000\u0000" + // 18640 - 18649
                "\u0000\u0000\u0000\uE4F5\u0000\uEBF0\uF1D6\u0000\u0000\uE5E2" + // 18650 - 18659
                "\u0000\uCCCC\u0000\uA5C8\uA5C9\uA5CA\uA5CB\uA5CC\uA5CD\uA5CE" + // 18660 - 18669
                "\uA5CF\uDDB2\u0000\u0000\u0000\u0000\uE6A9\u0000\uEFF3\uFDE9" + // 18670 - 18679
                "\u0000\uCFC1\u0000\uE0DF\uDEEC\u0000\u0000\uD0D7\u0000\u0000" + // 18680 - 18689
                "\u0000\u0000\u0000\u0000\uE6AC\u0000\uCEDE\uD7FC\u0000\uEDBB" + // 18690 - 18699
                "\u0000\u0000\uF6AB\u0000\u0000\uF5A7\u0000\u0000\u0000\u0000" + // 18700 - 18709
                "\u0000\u0000\uF8AF\uEFF6\u0000\uDFA1\uDDE1\u0000\u0000\u0000" + // 18710 - 18719
                "\u0000\u0000\u0000\uF2CD\u0000\u0000\uE8C6\u0000\u0000\u0000" + // 18720 - 18729
                "\u0000\u0000\u0000\uE8E5\u0000\u0000\uCAAA\uE1F9\u0000\uEAB1" + // 18730 - 18739
                "\u0000\u0000\u0000\uD5BE\u0000\u0000\u0000\u0000\uC6FB\uC6FC" + // 18740 - 18749
                "\u0000\uC6FD\uD5C4\u0000\u0000\u0000\u0000\u0000\u0000\uEDF4" + // 18750 - 18759
                "\uD4EB\u0000\uDEA2\u0000\u0000\u0000\uE5E6\u0000\uCED3\u0000" + // 18760 - 18769
                "\u0000\u0000\u0000\uDCF7\u0000\u0000\uD7A5\u0000\u0000\u0000" + // 18770 - 18779
                "\u0000\uF7E8\u0000\uA2A8\uA2AB\uA2AA\uA2AD\uA2A6\uA2A9\u0000" + // 18780 - 18789
                "\u0000\uCCCD\u0000\uDAFA\u0000\uF6CF\u0000\uE9B8\uF8B3\u0000" + // 18790 - 18799
                "\u0000\u0000\u0000\u0000\u0000\u0000\uCFEF\uCEEF\u0000\u0000" + // 18800 - 18809
                "\uF2F3\u0000\u0000\u0000\u0000\uC8B8\uC8B9\u0000\u0000\uEDB8" + // 18810 - 18819
                "\u0000\u0000\u0000\uDBB6\u0000\u0000\uFDF8\uFDF9\u0000\u0000" + // 18820 - 18829
                "\u0000\uF6BF\u0000\uF7DE\u0000\u0000\u0000\u0000\u0000\u0000" + // 18830 - 18839
                "\u0000\uA9FA\uCCAE\u0000\uDADB\u0000\u0000\u0000\u0000\uCDC7" + // 18840 - 18849
                "\uDBB9\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD0B4\uEDF3" + // 18850 - 18859
                "\uDCD9\uE0CD\u0000\u0000\u0000\u0000\uF7DA\uE9A6\uCBF2\u0000" + // 18860 - 18869
                "\u0000\u0000\u0000\u0000\u0000\uFBE2\u0000\uCDD3\uDDAF\uDDB0" + // 18870 - 18879
                "\u0000\u0000\uCBB7\uE8D3\u0000\u0000\uE0B5\u0000\u0000\u0000" + // 18880 - 18889
                "\u0000\u0000\u0000\uE3CE\u0000\u0000\uE6F0\u0000\u0000\u0000" + // 18890 - 18899
                "\u0000\u0000\u0000\uD0AD\u0000\u0000\uFBC7\uD5C8\u0000\uD7DF" + // 18900 - 18909
                "\u0000\uDDA9\u0000\uCCA7\uEAC9\u0000\u0000\u0000\u0000\u0000" + // 18910 - 18919
                "\uF8B6\uE8D2\u0000\uCAC5\uCCEB\u0000\u0000\u0000\u0000\uC7EC" + // 18920 - 18929
                "\uC7ED\u0000\u0000\uDDF8\u0000\u0000\u0000\u0000\u0000\uE8CF" + // 18930 - 18939
                "\uD8E6\u0000\uF4B1\u0000\u0000\u0000\u0000\u0000\uB4AE\uB4AF" + // 18940 - 18949
                "\u0000\uB4B0\uFDD8\u0000\u0000\u0000\u0000\uD2F6\u0000\u0000" + // 18950 - 18959
                "\uD1B3\u0000\u0000\u0000\u0000\u0000\uEFED\uF5C6\u0000\uE1A2" + // 18960 - 18969
                "\uE9C6\u0000\u0000\u0000\uF2C5\uDEBD\u0000\uF6A9\u0000\u0000" + // 18970 - 18979
                "\u0000\uDAA4\u0000\uCBDA\u0000\uE7D2\uD7C3\uF6F0\uE8DE\u0000" + // 18980 - 18989
                "\u0000\uD8B4\u0000\u0000\u0000\u0000\u0000\u0000\uF5B7\uE0F0" + // 18990 - 18999
                "\u0000\uA2B0\u0000\uA2C1\u0000\u0000\u0000\u0000\u0000\u0000" + // 19000 - 19009
                "\uA2A7\uE9FC\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uEDB6" + // 19010 - 19019
                "\uD2B1\u0000\u0000\u0000\u0000\uCCE9\u0000\uD9C4\uE9A5\uD6D5" + // 19020 - 19029
                "\u0000\uCDC5\u0000\uEDBA\uD1BD\u0000\uDCE4\u0000\uE5EF\u0000" + // 19030 - 19039
                "\u0000\u0000\u0000\u0000\uF6DE\u0000\u0000\u0000\uE5C7\uD6AC" + // 19040 - 19049
                "\u0000\u0000\u0000\uE8CB\u0000\u0000\uF8DD\u0000\uA9A8\uA8A9" + // 19050 - 19059
                "\uA9A9\u0000\u0000\u0000\u0000\u0000\uD6FE\u0000\u0000\u0000" + // 19060 - 19069
                "\uDEDB\u0000\u0000\uEFDE\u0000\u0038\u0039\u003A\u003B\u003C" + // 19070 - 19079
                "\u003D\u003E\u003F\uF9D7\u0000\u0000\u0000\u0000\u0000\u0000" + // 19080 - 19089
                "\u0000\uD9EE\uF6A8\uDDFD\u0000\u0000\u0000\u0000\u0000\u0000" + // 19090 - 19099
                "\uD4E9\u0000\u0000\uE2B2\u0000\uD4BD\u0000\u0000\uD9CE\u0000" + // 19100 - 19109
                "\uFAEF\uE3E1\u0000\u0000\u0000\u0000\u0000\u0000\uCDBA\u0000" + // 19110 - 19119
                "\u0000\uDECF\u0000\u0000\u0000\u0000\u0000\u0000\uF0CF\uF3BE" + // 19120 - 19129
                "\uE2AC\uD7E8\uCBD8\u0000\u0000\u0000\uE9E2\u0000\u0000\uE0FD" + // 19130 - 19139
                "\u0000\u0000\uD8F8\u0000\u0000\u0000\uBDC8\uBDC9\uBDCA\u0000" + // 19140 - 19149
                "\uBDCB\uE0CC\uEBF9\u0000\u0000\u0000\u0000\u0000\u0000\uF0E7" + // 19150 - 19159
                "\uE2CC\u0000\uF3A7\u0000\u0000\uCDEA\u0000\uEBEE\u0000\u0000" + // 19160 - 19169
                "\uECF5\uE8EE\u0000\uCBA9\uF1AF\u0000\u0000\uEACE\u0000\uE8DF" + // 19170 - 19179
                "\u0000\u0000\u0000\u0000\uC1C4\u0000\u0000\u0000\uECB9\u0000" + // 19180 - 19189
                "\u0000\u0000\u0000\uC6DF\uC6E0\u0000\uC6E1\uD1BC\u0000\uE5CF" + // 19190 - 19199
                "\u0000\uCBB6\u0000\uDAB8\u0000\uF6B6\u0000\uCEC2\uD6C7\u0000" + // 19200 - 19209
                "\uE3B4\u0000\uF1AD\uDBEB\u0000\uDFFE\u0000\u0000\uD8E1\u0000" + // 19210 - 19219
                "\uF7F3\uCEB8\u0000\u0000\u0000\uD8D2\uF9D6\u0000\u0000\uF2D7" + // 19220 - 19229
                "\u0000\uCAF8\uDAEF\u0000\u0000\uD6D4\uF9FC\u0000\uCCC0\u0000" + // 19230 - 19239
                "\u0000\u0000\u0000\u0000\uB3DB\uB3DC\u0000\uB3DD\uF3C5\u0000" + // 19240 - 19249
                "\u0000\uD4C0\uD5BF\u0000\u0000\u0000\uD5AF\u0000\u0000\u0000" + // 19250 - 19259
                "\uD6F5\u0000\uDBD8\u0000\u0000\uCAA2\u0000\u0000\uD1CD\u0000" + // 19260 - 19269
                "\uA9A7\u0000\u0000\u0000\u0000\u0000\u0000\uA8A8\uF0DB\u0000" + // 19270 - 19279
                "\u0000\u0000\u0000\u0000\u0000\u0000\uEBC9\uF8BA\uE8D0\u0000" + // 19280 - 19289
                "\u0000\uD8FB\u0000\u0000\uEAD5\uF4F3\uDAC9\u0000\uE6DE\u0000" + // 19290 - 19299
                "\u0000\u0000\u0000\uC7C7\uC7C8\u0000\u0000\uF8DE\uF9AA\uCAF7" + // 19300 - 19309
                "\u0000\uEDB7\u0000\u0000\uCCDE\u0000\u0000\u0000\u0000\u0000" + // 19310 - 19319
                "\u0000\uD8D5\u0000\u0000\uD7B5\u0000\u0000\u0000\u0000\u0000" + // 19320 - 19329
                "\u0000\uF4E9\uD6EC\uEBD3\uF9E5\u0000\uE0CA\u0000\u0000\uF2FD" + // 19330 - 19339
                "\uD3B0\u0000\uF1A5\uE9CE\u0000\u0000\u0000\uF9BC\u0000\u0000" + // 19340 - 19349
                "\uEDAB\u0000\uE3B7\u0000\u0000\u0000\u0000\uC1B5\u0000\u0000" + // 19350 - 19359
                "\u0000\uCAE5\u0000\u0000\u0000\uDCA1\uD7C6\u0000\uD1BB\uF7AA" + // 19360 - 19369
                "\u0000\uEDCA\uD7D3\uD8FA\uD6E0\u0000\uF1C6\u0000\u0000\u0000" + // 19370 - 19379
                "\u0000\u0000\uB3A2\uB3A3\u0000\u0000\uD7BA\u0000\uF2D5\uF5E5" + // 19380 - 19389
                "\uD9EF\u0000\u0000\uF8EE\u0000\u0000\u0000\uDEB5\u0000\u0000" + // 19390 - 19399
                "\uDCC7\u0000\u0000\u0000\u0000\u0000\uE1A9\uEBD6\u0000\uECDF" + // 19400 - 19409
                "\u0000\u0000\u0000\uDFFC\u0000\uF8CF\u0000\u0000\u0000\u0000" + // 19410 - 19419
                "\uEAC8\uEEB8\uF1AC\uCCBF\u0000\u0000\u0000\u0000\u0000\u0000" + // 19420 - 19429
                "\u0000\uE7BD\uE1AC\u0000\u0000\uE3EB\u0000\uEEC7\u0000\u0000" + // 19430 - 19439
                "\uF1F7\u0000\u0000\u0000\uE8B8\u0000\u0000\uE5C8\u0000\u0000" + // 19440 - 19449
                "\u0000\uFBA4\uD4B9\u0000\uE3DF\u0000\uDEC3\u0000\uDEC4\uCAA1" + // 19450 - 19459
                "\u0000\u0000\uD9D8\u0000\u0000\u0000\u0000\u0000\u0000\uE0C3" + // 19460 - 19469
                "\u0000\u0000\uD8D9\u0000\uF4A3\u0000\u0000\uF4DD\u0000\uA9AA" + // 19470 - 19479
                "\u0000\u0000\u0000\u0000\u0000\uA9AD\u0000\u0030\u0031\u0032" + // 19480 - 19489
                "\u0033\u0034\u0035\u0036\u0037\uE1C8\uDBB7\uDFE3\u0000\u0000" + // 19490 - 19499
                "\u0000\u0000\u0000\uB2F1\uB2F2\u0000\u0000\uDCBA\u0000\u0000" + // 19500 - 19509
                "\uCCB4\u0000\u0000\u0000\uDEAC\u0000\u0000\u0000\u0000\uF1DA" + // 19510 - 19519
                "\u0000\uFAF2\u0000\u0000\uDEB4\u0000\u0000\u0000\u0000\u0000" + // 19520 - 19529
                "\u0000\uF6EC\uDADC\uFAE4\uCEDC\uF2B5\uD0E4\uDDD1\u0000\u0000" + // 19530 - 19539
                "\u0000\u0000\uC7B9\u0000\u0000\uC7BA\uD2EF\u0000\u0000\u0000" + // 19540 - 19549
                "\uE2ED\u0000\u0000\uDEE9\uFCBC\u0000\uDAA2\uDAA3\u0000\uD2A1" + // 19550 - 19559
                "\u0000\u0000\uE2DB\u0000\u0000\u0000\uDFDE\u0000\uE0C7\uD1BA" + // 19560 - 19569
                "\u0000\uF1C4\u0000\uE5B3\uFBF5\uE9E1\uFDE0\uCBB3\u0000\u0000" + // 19570 - 19579
                "\u0000\u0000\u0000\u0000\uD5DD\uEFC4\u0000\u0000\u0000\u0000" + // 19580 - 19589
                "\u0000\u0000\uE1D8\uD6EB\u0000\u0000\u0000\uF4D9\u0000\u0000" + // 19590 - 19599
                "\u0000\uA2C6\u0000\u0000\u0000\u0000\u0000\uD4F6\uE5B7\u0000" + // 19600 - 19609
                "\u0000\uD6A2\u0000\uEDF0\u0000\u0000\u0000\u0000\uC5E2\u0000" + // 19610 - 19619
                "\u0000\u0000\uFCDC\u0000\u0000\u0000\u0000\uDCD5\u0000\uF7B5" + // 19620 - 19629
                "\uFCF3\uF0F3\uE1C6\u0000\u0000\u0000\uD4BF\u0000\u0000\u0000" + // 19630 - 19639
                "\uA1F8\uA1F9\u0000\u0000\uA1F6\uA1F7\uEFBD\uFCD6\u0000\u0000" + // 19640 - 19649
                "\uDBF4\u0000\uEFAA\uF8B9\uEEC6\u0000\u0000\u0000\u0000\u0000" + // 19650 - 19659
                "\u0000\u0000\uFDC8\uD0B5\u0000\u0000\u0000\u0000\u0000\u0000" + // 19660 - 19669
                "\u0000\uECCB\uCECC\uF5E8\uF7D5\u0000\uD3CD\u0000\uF3FE\u0000" + // 19670 - 19679
                "\uDBE9\uFDCC\u0000\u0000\u0000\u0000\u0000\u0000\uE1DF\u0000" + // 19680 - 19689
                "\uDAD1\uD8A2\u0000\u0000\u0000\u0000\u0000\uDDAC\u0000\uE4E6" + // 19690 - 19699
                "\u0000\uF1EA\u0000\u0000\u0000\uCBEC\uCBC0\uEDF1\u0000\u0000" + // 19700 - 19709
                "\uF1E2\u0000\uD4DB\u0000\u0000\uFBA8\uD0A8\u0000\u0000\uDAEC" + // 19710 - 19719
                "\u0000\u0000\uE7B8\u0000\u0000\u0000\u0000\u0000\u0000\uCFC0" + // 19720 - 19729
                "\u0000\uE6A8\uCFD7\u0000\u0000\u0000\u0000\u0000\u0000\uCFDF" + // 19730 - 19739
                "\uE9A1\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uD7A9\uE5CD" + // 19740 - 19749
                "\u0000\u0000\u0000\uFAEB\u0000\uCFBC\u0000\uDCF5\uE0B9\u0000" + // 19750 - 19759
                "\u0000\u0000\uD4CE\u0000\uF4B5\uCDDA\u0000\u0000\u0000\u0000" + // 19760 - 19769
                "\u0000\uD9CF\u0000\uE4A7\uECD2\u0000\u0000\uF6B1\u0000\u0000" + // 19770 - 19779
                "\uCEFB\uF0D6\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uF4F2" + // 19780 - 19789
                "\uCAD9\u0000\u0000\uEFEF\u0000\uF5AA\u0000\u0000\uFADD\u0000" + // 19790 - 19799
                "\u0000\u0000\u0000\u0000\u0000\uEBD7\u0000\u0000\uE2AB\uF3E8" + // 19800 - 19809
                "\u0000\u0000\u0000\u0000\u0000\uB0FA\uB0FB\u0000\u0000\uFCCF" + // 19810 - 19819
                "\uFBA2\u0000\uE0DC\u0000\u0000\u0000\uF0EC\u0000\u0000\u0000" + // 19820 - 19829
                "\u0000\uCCF6\u0000\u0000\uD3BA\u0000\uFAFB\u0000\u0000\uFABD" + // 19830 - 19839
                "\uCCC8\uEFCD\uD5D5\u0000\uA9A3\u0000\u0000\u0000\u0000\u0000" + // 19840 - 19849
                "\u0000\uA1C0\uFDFC\u0000\u0000\u0000\u0000\uE1AA\u0000\u0000" + // 19850 - 19859
                "\uE8CC\u0000\u0000\u0000\uDEB7\u0000\u0000\uCBC9\u0000\u0000" + // 19860 - 19869
                "\uE6D1\uF0CC\u0000\u0000\uDEDE\uF2AF\uF8A9\u0000\u0000\u0000" + // 19870 - 19879
                "\u0000\uC4C4\uC4C5\u0000\uC4C6\uDDFA\u0000\u0000\u0000\u0000" + // 19880 - 19889
                "\u0000\u0000\uF0D5\uE2B3\uDEE7\u0000\u0000\u0000\u0000\u0000" + // 19890 - 19899
                "\u0000\uD4B4\u0000\u0000\uCEEA\u0000\u0000\u0000\u0000\u0000" + // 19900 - 19909
                "\u0000\uF2F4\u0000\u0000\uF2D1\u0000\u0000\u0000\u0000\uE9C1" + // 19910 - 19919
                "\u0000\uD0E6\u0000\u0000\uDEC1\u0000\u0000\uE4AC\u0000\uA8AA" + // 19920 - 19929
                "\u0000\u0000\u0000\u0000\u0000\uA8AD\uA9AC\uF8F9\u0000\u0000" + // 19930 - 19939
                "\u0000\u0000\uF0AE\u0000\u0000\uDAAA\u0000\u0000\u0000\u0000" + // 19940 - 19949
                "\u0000\u0000\uF8CC\u0000\uEAD9\uDEE6\u0000\u0000\u0000\u0000" + // 19950 - 19959
                "\uDFD4\u0000\u0000\uD9C0\uD6EF\u0000\u0000\u0000\u0000\uD9CC" + // 19960 - 19969
                "\uE9DE\u0000\u0000\u0000\u0000\u0000\uF0D3\uF2B4\uD1B7\uF2B3" + // 19970 - 19979
                "\u0000\u0000\u0000\u0000\u0000\u0000\uF5B1\u0000\u0000\uE7A7" + // 19980 - 19989
                "\u0000\uE6D7\u0000\u0000\u0000\u0000\uC5AD\uC5AE\u0000\u0000" + // 19990 - 19999
                "\uE1D7\u0000\uE6CF\u0000\uF4F1\u0000\u0000\uDAC5\u0000\uD8EC" + // 20000 - 20009
                "\u0000\u0000\u0000\u0000\uC4BC\uC4BD\u0000\u0000\uF8F6\u0000" + // 20010 - 20019
                "\u0000\u0000\u0000\uF5D2\uEDE9\uDEE5\uD1B5\u0000\u0000\u0000" + // 20020 - 20029
                "\u0000\u0000\uD1B6\uD8A8\u0000\u0000\u0000\uCCE4\u0000\u0000" + // 20030 - 20039
                "\uD1B4\uDAF1\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE9C0" + // 20040 - 20049
                "\uCAF9\u0000\u0000\uD4DA\u0000\u0000\u0000\u0000\uC6BA\uC6BB" + // 20050 - 20059
                "\u0000\u0000\uFDF7\u0000\u0000\u0000\u0000\u0000\uE2D9\uF3B7" + // 20060 - 20069
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uDFB6\uD9C2\u0000" + // 20070 - 20079
                "\uF0D2\u0000\uE4D1\u0000\u0000\u0000\uBECE\uBECF\uBED0\u0000" + // 20080 - 20089
                "\uBED1\uD8F9\u0000\u0000\u0000\u0000\u0000\u0000\u0000\uE4F3" + // 20090 - 20099
                "\uF7AF\uDAB6\u0000\uCAD7\u0000\u0000\u0000\u0000\uC6A1\u0000" + // 20100 - 20109
                "\u0000\u0000\uBDD7\uBDD8\uBDD9\u0000\u0000\uDBE1\u0000\u0000" + // 20110 - 20119
                "\uE5C9\u0000\uEDB4\u0000\uEAA7\uE9F6\uFBBB\u0000\uE7E9\uEFCC" + // 20120 - 20129
                "\u0000\u0000\uE3F7\u0000\u0000\u0000\u0000\u0000\uECA8\uDFD3" + // 20130 - 20139
                "\u0000\u0000\u0000\uDAF0\u0000\uE2EA\u0000\uE5F8\u0000\u0000" + // 20140 - 20149
                "\uDEC0\uECA3\u0000\uE9CD\u0000\uA8A2\u0000\u0000\u0000\u0000" + // 20150 - 20159
                "\u0000\u0000\uA1BF\uE7C3\u0000\uECCC\u0000\u0000\u0000\u0000" + // 20160 - 20169
                "\u0000\uB2BF\uB2C0\u0000\u0000\uE6D5\u0000\u0000\uE9F2\u0000" + // 20170 - 20179
                "\uDFB0\u0000\uE3AB\uEBE0\u0000\u0000\u0000\uCEFA\uCBF7\uE5A5" + // 20180 - 20189
                "\uCDC1\u0000\u0000\uFBD3\u0000\u0000\u0000\u0000\uC5FE\u0000" + // 20190 - 20199
                "\u0000\u0000\uB8EB\uB8EC\uB8ED\u0000\uB8EE\uCDA4\u0000\u0000" + // 20200 - 20209
                "\uD4F4\uDBA1\uDBDC\uDBDD\u0000\uFCAF\uD3A1\u0000\uF1AB\u0000" + // 20210 - 20219
                "\u0000\u0000\u0000\uBEC6\uBEC7\u0000\u0000\uF4B8\uF7BC\uDCFD" + // 20220 - 20229
                "\u0000\uE8EC\uE4E7\u0000\uA2AC\uA9F6\uA8AC\u0000\uA8F9\uA8F6" + // 20230 - 20239
                "\uA8FA\uA2AF\uE8B9\u0000\uEFA6\u0000\u0000\u0000\u0000\u0000" + // 20240 - 20249
                "\uB2B8\u0000\u0000\u0000\uB6D5\uB6D6\u0000\u0000\u0000\uB8DD" + // 20250 - 20259
                "\uB8DE\uB8DF\u0000\u0000\uD6A1\uFDBF\u0000\uFCD3\u0000\uEFA1" + // 20260 - 20269
                "\u0000\uCEF8\uDCB0\u0000\u0000\u0000\u0000\uE3AA\u0000\uA1C6" + // 20270 - 20279
                "\uA1BE\uA9F7\uA9F8\uA2A5\u0000\uA2D2\uA1A4\u0028\u0029\u002A" + // 20280 - 20289
                "\u002B\u002C\u002D\u002E\u002F\uF4B0\uF3EA\uDAEE\u0000\uD7BB" + // 20290 - 20299
                "\u0000\uE2B1\u0000\uEDDB\uDFB2\uDFBE\uF9BB\u0000\uDCF4\u0000" + // 20300 - 20309
                "\u0000\uF9C9\u0000\uE4E2\u0000\uFBBD\u0000\u0000\uECEC\uFBBE" + // 20310 - 20319
                "\uDFEB\u0000\uE1F8\u0000\u0000\uE2D4\uD2FD\u0000\uE5A8\u0000" + // 20320 - 20329
                "\u0000\u0000\uE0FC\uD4A8\u0000\uEDD3\uD8EF\uE5DC\u0000\u0000" + // 20330 - 20339
                "\u0000\u0000\u0000\u0000\u0000\uE6A2\uD3C3\u0000\uD8A6\u0000" + // 20340 - 20349
                "\uF6C1\u0000\u0000\u0000\uB3D6\uB3D7\uB3D8\u0000\u0000\uE5EA" + // 20350 - 20359
                "\u0000\u0000\uF1E0\u0000\u0000\u0000\uEFB5\u0000\u0000\u0000" + // 20360 - 20369
                "\uFAE9\uD4E3\uCCE2\u0000\uF7D4\u0000\u0000\u0000\u0000\uC5F5" + // 20370 - 20379
                "\uC5F6\u0000\u0000\uF8B2\u0000\u0000\u0000\uDCEB\u0000\u0000" + // 20380 - 20389
                "\uEFBA\uF1DD\u0000\uDEB3\u0000\u0000\u0000\uCECE\u0000\u0000" + // 20390 - 20399
                "\u0000\u0000\uDECB\uF6B8\u0000\u0000\u0000\uCAC8\uF9EE\uDBEC" + // 20400 - 20409
                "\u0000\u0000\uFDC3\u0000\u0000\u0000\uEBF6\uCFB2\u0000\uECE9" + // 20410 - 20419
                "\uEFCB\u0000\uF6D2\u0000\u0000\u0000\uD8B2\uD3B8\uF2D6\u0000" + // 20420 - 20429
                "\u0000\uD4D9\uEEC5\uF2F0\u0000\uA2E4\u0000\u0000\uA7E4\uA7EE" + // 20430 - 20439
                "\uA7E9\u0000\u0000\uE9BA\u0000\u0000\u0000\u0000\u0000\u0000" + // 20440 - 20449
                "\uCCDD\u0000\u0000\uDBFA\u0000\u0000\u0000\uE8B5\u0000\uD3A6" + // 20450 - 20459
                "\uD1B1\u0000\uCBB1\u0000\u0000\u0000\u0000\uD1B2\uECB6\u0000" + // 20460 - 20469
                "\u0000\u0000\u0000\uFBFE\uD3D7\u0000\uA7EA\u0000\u0000\uA7EB" + // 20470 - 20479
                "\u0000\u0000\uA7DF\u0000\uA1A7\u0000\uA8A3\u0000\u0000\uA1A9" + // 20480 - 20489
                "\u0000\u0000\uE2F1\u0000\u0000\u0000\uD2EE\u0000\u0000\uE1E6" + // 20490 - 20499
                "\uF7F9\uEFA4\u0000\uEFEB\u0000\u0000\u0000\u0000\u0000\uB2A7" + // 20500 - 20509
                "\u0000\u0000\u0000\uB1BF\uB1C0\uB1C1\u0000\uB1C2\uEFA3\uEBA6" + // 20510 - 20519
                "\uCBA3\uE3E9\u0000\u0000\u0000\uD1FB\uE9C4\u0000\u0000\uDCCB" + // 20520 - 20529
                "\uE9C5\u0000\u0000\u0000\uB0C0\u0000\uB0C1\u0000\u0000\uDEE1" + // 20530 - 20539
                "\u0000\uE4A3\u0000\u0000\u0000\uD7B7\uCAF6\u0000\uE4A4\uF4D6" + // 20540 - 20549
                "\u0000\u0000\u0000\uDFE6\uFBD2\u0000\uF8F8\uF7FB\u0000\u0000" + // 20550 - 20559
                "\uE8BF\u0000\uA7BC\uA7ED\uA7B5\u0000\u0000\u0000\u0000\uA7B9" + // 20560 - 20569
                "\uCEB6\u0000\uF3C0\u0000\uCDFE\u0000\u0000\u0000\uCDD5\u0000" + // 20570 - 20579
                "\u0000\u0000\u0000\uDCB3\u0000\u0000\u0000\u0000\uC1D6\uC1D7" + // 20580 - 20589
                "\u0000\u0000\uFBCD\u0000\uD5BD\uF1DF\u0000\u0000\uF6FB\uFCBB" + // 20590 - 20599
                "\u0000\uE2B0\u0000\u0000\uE6A5\u0000\u0000\uD3C2\u0000\u0000" + // 20600 - 20609
                "\u0000\u0000\uD3B6\u0000\uA7DA\uA7DB\uA2E3\uA7EC\uA7A6\uA7E0" + // 20610 - 20619
                "\uA7EF\uA2E1\uF3BF\u0000\uF0D1\u0000\u0000\u0000\u0000\u0000" + // 20620 - 20629
                "\uB2A5\uB2A6\u0000\u0000\uEEFE\u0000\uE7FE\u0000\u0000\u0000" + // 20630 - 20639
                "\u0000\uC4F5\u0000\u0000\u0000\uF9DB\u0000\u0000\u0000\u0000" + // 20640 - 20649
                "\uCCD1\u0000\uDFEA\u0000\u0000\uF4BB\uDAD5\u0000\uF9B2\u0000" + // 20650 - 20659
                "\u0000\u0000\uEFB2\u0000\u0000\u0000\u0000\uF9E1\u0000\u0000" + // 20660 - 20669
                "\u0000\u0000\uC0A7\uC0A8\u0000\u0000\uEFE0\u0000\u0000\u0000" + // 20670 - 20679
                "\uE5E5\uD0D5\u0000\uA7C7\uA7C8\uA7CE\uA7CF\uA7D0\uA7D1\uA7D2" + // 20680 - 20689
                "\uA7D3\uD7F3\u0000\u0000\u0000\uFCD4\u0000\uDAD7\uCCDF\uF2D3" + // 20690 - 20699
                "\uFBA9\uD8A5\u0000\u0000\u0000\u0000\uD5CB\u0000\u0000\u0000" + // 20700 - 20709

                "\u0000\uA3DC\u0000\u0000\u0000\u0000\u0000\u0000" // 20710 - 20719 Bug 4199604
                ;
        }
    }
}
