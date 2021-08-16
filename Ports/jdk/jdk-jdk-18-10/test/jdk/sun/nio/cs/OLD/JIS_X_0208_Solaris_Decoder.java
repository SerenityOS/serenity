/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

public class JIS_X_0208_Solaris_Decoder extends DoubleByteDecoder
{

    public JIS_X_0208_Solaris_Decoder(Charset cs) {
        super(cs,
              index1,
              index2,
              0x21,
              0x7E);
    }
    private final static String innerIndex0=
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\u2460\u2461"+
        "\u2462\u2463\u2464\u2465\u2466\u2467\u2468\u2469"+
        "\u246A\u246B\u246C\u246D\u246E\u246F\u2470\u2471"+
        "\u2472\u2473\u2160\u2161\u2162\u2163\u2164\u2165"+
        "\u2166\u2167\u2168\u2169\uFFFD\u3349\u3314\u3322"+
        "\u334D\u3318\u3327\u3303\u3336\u3351\u3357\u330D"+
        "\u3326\u3323\u332B\u334A\u333B\u339C\u339D\u339E"+
        "\u338E\u338F\u33C4\u33A1\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\u337B\u301D\u301F\u2116"+
        "\u33CD\u2121\u32A4\u32A5\u32A6\u32A7\u32A8\u3231"+
        "\u3232\u3239\u337E\u337D\u337C\u2252\u2261\u222B"+
        "\u222E\u2211\u221A\u22A5\u2220\u221F\u22BF\u2235"+
        "\u2229\u222A\uFFFD\uFFFD\u7E8A\u891C\u9348\u9288"+
        "\u84DC\u4FC9\u70BB\u6631\u68C8\u92F9\u66FB\u5F45"+
        "\u4E28\u4EE1\u4EFC\u4F00\u4F03\u4F39\u4F56\u4F92"+
        "\u4F8A\u4F9A\u4F94\u4FCD\u5040\u5022\u4FFF\u501E"+
        "\u5046\u5070\u5042\u5094\u50F4\u50D8\u514A\u5164"+
        "\u519D\u51BE\u51EC\u5215\u529C\u52A6\u52C0\u52DB"+
        "\u5300\u5307\u5324\u5372\u5393\u53B2\u53DD\uFA0E"+
        "\u549C\u548A\u54A9\u54FF\u5586\u5759\u5765\u57AC"+
        "\u57C8\u57C7\uFA0F\uFA10\u589E\u58B2\u590B\u5953"+
        "\u595B\u595D\u5963\u59A4\u59BA\u5B56\u5BC0\u752F"+
        "\u5BD8\u5BEC\u5C1E\u5CA6\u5CBA\u5CF5\u5D27\u5D53"+
        "\uFA11\u5D42\u5D6D\u5DB8\u5DB9\u5DD0\u5F21\u5F34"+
        "\u5F67\u5FB7\u5FDE\u605D\u6085\u608A\u60DE\u60D5"+
        "\u6120\u60F2\u6111\u6137\u6130\u6198\u6213\u62A6"+
        "\u63F5\u6460\u649D\u64CE\u654E\u6600\u6615\u663B"+
        "\u6609\u662E\u661E\u6624\u6665\u6657\u6659\uFA12"+
        "\u6673\u6699\u66A0\u66B2\u66BF\u66FA\u670E\uF929"+
        "\u6766\u67BB\u6852\u67C0\u6801\u6844\u68CF\uFA13"+
        "\u6968\uFA14\u6998\u69E2\u6A30\u6A6B\u6A46\u6A73"+
        "\u6A7E\u6AE2\u6AE4\u6BD6\u6C3F\u6C5C\u6C86\u6C6F"+
        "\u6CDA\u6D04\u6D87\u6D6F\u6D96\u6DAC\u6DCF\u6DF8"+
        "\u6DF2\u6DFC\u6E39\u6E5C\u6E27\u6E3C\u6EBF\u6F88"+
        "\u6FB5\u6FF5\u7005\u7007\u7028\u7085\u70AB\u710F"+
        "\u7104\u715C\u7146\u7147\uFA15\u71C1\u71FE\u72B1"+
        "\u72BE\u7324\uFA16\u7377\u73BD\u73C9\u73D6\u73E3"+
        "\u73D2\u7407\u73F5\u7426\u742A\u7429\u742E\u7462"+
        "\u7489\u749F\u7501\u756F\u7682\u769C\u769E\u769B"+
        "\u76A6\uFA17\u7746\u52AF\u7821\u784E\u7864\u787A"+
        "\u7930\uFA18\uFA19\uFA1A\u7994\uFA1B\u799B\u7AD1"+
        "\u7AE7\uFA1C\u7AEB\u7B9E\uFA1D\u7D48\u7D5C\u7DB7"+
        "\u7DA0\u7DD6\u7E52\u7F47\u7FA1\uFA1E\u8301\u8362"+
        "\u837F\u83C7\u83F6\u8448\u84B4\u8553\u8559\u856B"+
        "\uFA1F\u85B0\uFA20\uFA21\u8807\u88F5\u8A12\u8A37"+
        "\u8A79\u8AA7\u8ABE\u8ADF\uFA22\u8AF6\u8B53\u8B7F"+
        "\u8CF0\u8CF4\u8D12\u8D76\uFA23\u8ECF\uFA24\uFA25"+
        "\u9067\u90DE\uFA26\u9115\u9127\u91DA\u91D7\u91DE"+
        "\u91ED\u91EE\u91E4\u91E5\u9206\u9210\u920A\u923A"+
        "\u9240\u923C\u924E\u9259\u9251\u9239\u9267\u92A7"+
        "\u9277\u9278\u92E7\u92D7\u92D9\u92D0\uFA27\u92D5"+
        "\u92E0\u92D3\u9325\u9321\u92FB\uFA28\u931E\u92FF"+
        "\u931D\u9302\u9370\u9357\u93A4\u93C6\u93DE\u93F8"+
        "\u9431\u9445\u9448\u9592\uF9DC\uFA29\u969D\u96AF"+
        "\u9733\u973B\u9743\u974D\u974F\u9751\u9755\u9857"+
        "\u9865\uFA2A\uFA2B\u9927\uFA2C\u999E\u9A4E\u9AD9"+
        "\u9ADC\u9B75\u9B72\u9B8F\u9BB1\u9BBB\u9C00\u9D70"+
        "\u9D6B\uFA2D\u9E19\u9ED1\uFFFD\uFFFD\u2170\u2171"+
        "\u2172\u2173\u2174\u2175\u2176\u2177\u2178\u2179"+
        "\u3052\u00A6\uFF07\uFF02\u2170\u2171\u2172\u2173"+
        "\u2174\u2175\u2176\u2177\u2178\u2179\u2160\u2161"+
        "\u2162\u2163\u2164\u2165\u2166\u2167\u2168\u2169"+
        "\u3052\u00A6\uFF07\uFF02\u3231\u2116\u2121\u306E"+
        "\u7E8A\u891C\u9348\u9288\u84DC\u4FC9\u70BB\u6631"+
        "\u68C8\u92F9\u66FB\u5F45\u4E28\u4EE1\u4EFC\u4F00"+
        "\u4F03\u4F39\u4F56\u4F92\u4F8A\u4F9A\u4F94\u4FCD"+
        "\u5040\u5022\u4FFF\u501E\u5046\u5070\u5042\u5094"+
        "\u50F4\u50D8\u514A\u5164\u519D\u51BE\u51EC\u5215"+
        "\u529C\u52A6\u52C0\u52DB\u5300\u5307\u5324\u5372"+
        "\u5393\u53B2\u53DD\uFA0E\u549C\u548A\u54A9\u54FF"+
        "\u5586\u5759\u5765\u57AC\u57C8\u57C7\uFA0F\uFA10"+
        "\u589E\u58B2\u590B\u5953\u595B\u595D\u5963\u59A4"+
        "\u59BA\u5B56\u5BC0\u752F\u5BD8\u5BEC\u5C1E\u5CA6"+
        "\u5CBA\u5CF5\u5D27\u5D53\uFA11\u5D42\u5D6D\u5DB8"+
        "\u5DB9\u5DD0\u5F21\u5F34\u5F67\u5FB7\u5FDE\u605D"+
        "\u6085\u608A\u60DE\u60D5\u6120\u60F2\u6111\u6137"+
        "\u6130\u6198\u6213\u62A6\u63F5\u6460\u649D\u64CE"+
        "\u654E\u6600\u6615\u663B\u6609\u662E\u661E\u6624"+
        "\u6665\u6657\u6659\uFA12\u6673\u6699\u66A0\u66B2"+
        "\u66BF\u66FA\u670E\uF929\u6766\u67BB\u6852\u67C0"+
        "\u6801\u6844\u68CF\uFA13\u6968\uFA14\u6998\u69E2"+
        "\u6A30\u6A6B\u6A46\u6A73\u6A7E\u6AE2\u6AE4\u6BD6"+
        "\u6C3F\u6C5C\u6C86\u6C6F\u6CDA\u6D04\u6D87\u6D6F"+
        "\u6D96\u6DAC\u6DCF\u6DF8\u6DF2\u6DFC\u6E39\u6E5C"+
        "\u6E27\u6E3C\u6EBF\u6F88\u6FB5\u6FF5\u7005\u7007"+
        "\u7028\u7085\u70AB\u710F\u7104\u715C\u7146\u7147"+
        "\uFA15\u71C1\u71FE\u72B1\u72BE\u7324\uFA16\u7377"+
        "\u73BD\u73C9\u73D6\u73E3\u73D2\u7407\u73F5\u7426"+
        "\u742A\u7429\u742E\u7462\u7489\u749F\u7501\u756F"+
        "\u7682\u769C\u769E\u769B\u76A6\uFA17\u7746\u52AF"+
        "\u7821\u784E\u7864\u787A\u7930\uFA18\uFA19\uFA1A"+
        "\u7994\uFA1B\u799B\u7AD1\u7AE7\uFA1C\u7AEB\u7B9E"+
        "\uFA1D\u7D48\u7D5C\u7DB7\u7DA0\u7DD6\u7E52\u7F47"+
        "\u7FA1\uFA1E\u8301\u8362\u837F\u83C7\u83F6\u8448"+
        "\u84B4\u8553\u8559\u856B\uFA1F\u85B0\uFA20\uFA21"+
        "\u8807\u88F5\u8A12\u8A37\u8A79\u8AA7\u8ABE\u8ADF"+
        "\uFA22\u8AF6\u8B53\u8B7F\u8CF0\u8CF4\u8D12\u8D76"+
        "\uFA23\u8ECF\uFA24\uFA25\u9067\u90DE\uFA26\u9115"+
        "\u9127\u91DA\u91D7\u91DE\u91ED\u91EE\u91E4\u91E5"+
        "\u9206\u9210\u920A\u923A\u9240\u923C\u924E\u9259"+
        "\u9251\u9239\u9267\u92A7\u9277\u9278\u92E7\u92D7"+
        "\u92D9\u92D0\uFA27\u92D5\u92E0\u92D3\u9325\u9321"+
        "\u92FB\uFA28\u931E\u92FF\u931D\u9302\u9370\u9357"+
        "\u93A4\u93C6\u93DE\u93F8\u9431\u9445\u9448\u9592"+
        "\uF9DC\uFA29\u969D\u96AF\u9733\u973B\u9743\u974D"+
        "\u974F\u9751\u9755\u9857\u9865\uFA2A\uFA2B\u9927"+
        "\uFA2C\u999E\u9A4E\u9AD9\u9ADC\u9B75\u9B72\u9B8F"+
        "\u9BB1\u9BBB\u9C00\u9D70\u9D6B\uFA2D\u9E19\u9ED1"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"+
        "\uFFFD\uFFFD";

    private static final short index1[] = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 4, 5, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 6, 7, 8, 9, 10, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private final static String index2[] = {
        innerIndex0
    };

    protected char convSingleByte(int b) {
        return REPLACE_CHAR;
    }


    static short[] getIndex1() {
       return index1;
    }

    static String[] getIndex2() {
       return index2;
    }
}
