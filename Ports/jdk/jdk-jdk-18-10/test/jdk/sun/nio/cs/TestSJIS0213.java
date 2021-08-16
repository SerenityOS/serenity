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
 * @bug 6529796 6710199
 * @summary Test SJIS/MS932_0213 charsets
 * @modules jdk.charsets
 */

import java.util.Arrays;

public class TestSJIS0213 {
    private static String sjisStr = "\u2014\u301C\u2016\u2212\u00A2\u00A3\u00AC";
    private static String winStr = "\u2015\uFF5E\u2225\uFF0D\uFFE0\uFFE1\uFFE2\u2252\u2261\u222B\u2211\u221A\u22A5\u2220\u2235\u2229\u222A";

    private static String compStr = "\u304B\u309A\u304D\u309A\u304F\u309A\u3051\u309A\u3053\u309A\u30AB\u309A\u30AD\u309A\u30AF\u309A\u30B1\u309A\u30B3\u309A\u30BB\u309A\u30C4\u309A\u30C8\u309A\u31F7\u309A\u00E6\u0300\u0254\u0300\u0254\u0301\u028C\u0300\u028C\u0301\u0259\u0300\u0259\u0301\u025A\u0300\u025A\u0301\u02E9\u02E5\u02E5\u02E9";
    private static byte[] compBytes = new byte[] {
        (byte)0x82, (byte)0xf5,
        (byte)0x82, (byte)0xf6,
        (byte)0x82, (byte)0xf7,
        (byte)0x82, (byte)0xf8,
        (byte)0x82, (byte)0xf9,
        (byte)0x83, (byte)0x97,
        (byte)0x83, (byte)0x98,
        (byte)0x83, (byte)0x99,
        (byte)0x83, (byte)0x9a,
        (byte)0x83, (byte)0x9b,
        (byte)0x83, (byte)0x9c,
        (byte)0x83, (byte)0x9d,
        (byte)0x83, (byte)0x9e,
        (byte)0x83, (byte)0xf6,
        (byte)0x86, (byte)0x63,
        (byte)0x86, (byte)0x67,
        (byte)0x86, (byte)0x68,
        (byte)0x86, (byte)0x69,
        (byte)0x86, (byte)0x6a,
        (byte)0x86, (byte)0x6b,
        (byte)0x86, (byte)0x6c,
        (byte)0x86, (byte)0x6d,
        (byte)0x86, (byte)0x6e,
        (byte)0x86, (byte)0x85,
        (byte)0x86, (byte)0x86 };

    private static String mixedStr = "\u002B\u0041\u007a\uff61\uff9f\u3000\u30a1\u4e00\u304B\u309A\u304b";
    private static byte[] mixedBytes = new byte[] {
         (byte)0x2b,
         (byte)0x41, (byte)0x7a,
         (byte)0xa1, (byte)0xdf,
         (byte)0x81, (byte)0x40,
         (byte)0x83, (byte)0x40,
         (byte)0x88, (byte)0xea,
         (byte)0x82, (byte)0xf5,   // composite
         (byte)0x82, (byte)0xa9 }; // base without cc

    //base + base + cc
    private static String mixedCompStr = "\u304D\u304B\u309A";
    private static byte[] mixedCompBytes = new byte[] {
        (byte)0x82, (byte)0xab, (byte)0x82, (byte)0xf5};

    private static char[] unmappableChars = new char[] {
        0x80, 0xfffc, 0xfffd};

    private static byte[] unmappableBytes = new byte[] {
        0x3f, 0x3f, 0x3f};

    public static void main(String[] args) throws Exception {
        if (!winStr.equals(new String(winStr.getBytes("MS932"), "MS932_0213")))
            throw new RuntimeException("MS932_0213 failed on special codepoints!");

        if (!(Arrays.equals(compStr.getBytes("MS932_0213"), compBytes)) ||
            !compStr.equals(new String(compBytes, "MS932_0213")))
            throw new RuntimeException("MS932_0213 failed on composites!");

        if (!(Arrays.equals(mixedStr.getBytes("MS932_0213"), mixedBytes)) ||
            !mixedStr.equals(new String(mixedBytes, "MS932_0213")))
            throw new RuntimeException("MS932_0213 failed on mixed!");

        if (!sjisStr.equals(new String(sjisStr.getBytes("SJIS"), "SJIS_0213")))
            throw new RuntimeException("SJIS_0213 failed on special codepoints!");

        if (!(Arrays.equals(compStr.getBytes("SJIS_0213"), compBytes)) ||
            !compStr.equals(new String(compBytes, "SJIS_0213")))
            throw new RuntimeException("SJIS_0213 failed on composites!");

        if (!(Arrays.equals(mixedStr.getBytes("SJIS_0213"), mixedBytes)) ||
            !mixedStr.equals(new String(mixedBytes, "SJIS_0213")))
            throw new RuntimeException("SJIS_0213 failed on mixed!");

        if (!(Arrays.equals(mixedCompStr.getBytes("SJIS_0213"), mixedCompBytes)) ||
            !mixedCompStr.equals(new String(mixedCompBytes, "SJIS_0213")))
            throw new RuntimeException("SJIS_0213 failed on mixedComp!");

        if (!Arrays.equals(new String(unmappableChars).getBytes("SJIS_0213"), unmappableBytes) ||
            !Arrays.equals(new String(unmappableChars).getBytes("MS932_0213"), unmappableBytes))
            throw new RuntimeException("SJIS/MS932_0213 failed on unmappable encoding!");
    }
}
