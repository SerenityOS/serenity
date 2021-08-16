/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4454622
 * @summary Check if all supported sun.io encoding names are supported in nio.charset
 * @modules jdk.charsets
 */

import java.util.HashMap;
import java.util.Set;
import java.io.UnsupportedEncodingException;
import java.nio.charset.*;

public class SunioAlias {
    public static void main(String argv[]) throws UnsupportedEncodingException {
        Set<String> keys = aliasTable.keySet();
        String s = "testing string";
        boolean failed = false;
        for (String alias : keys) {
            /* See if StringCoding works correctly without sun.io package */
            try {
                byte[] bs= s.getBytes(alias);
                new String(bs, alias);

                Charset csAlias = Charset.forName(alias);
                Charset csName = Charset.forName(aliasTable.get(alias));
                if (csName != csAlias) {
                    System.out.printf("Alias %s and %s is NOT the same charset in nio\n",
                                      alias, aliasTable.get(alias));
                }
            } catch (UnsupportedEncodingException e) {
                System.out.printf("Alias %s is UnsupportedEncoding\n", alias);
                failed = true;
            } catch (IllegalCharsetNameException e) {
                System.out.printf("Alias %s is IllegalCharsetName\n", alias);
                failed = true;
            }
        }
        if (failed)
            throw new UnsupportedEncodingException ("sun.io encoding names are not supported in nio.charset!");
    }
    //aliasTable is copy/pasted from sun.io.CharacterEncoding.java

    private static HashMap<String, String> aliasTable;
    static {
        aliasTable = new HashMap<String, String>(460, 1.0f);

        /* known failed names... TBD
        // JIS-defined Shift JIS
        aliasTable.put("\u30b7\u30d5\u30c8\u7b26\u53f7\u5316\u8868\u73fe",
                       "SJIS");
        // Specialized auto-detection for Japanese charsets
        aliasTable.put("jis auto detect",       "JISAutoDetect");
        // MIBenum: 1010
        aliasTable.put("unicode-1-1",       "UnicodeBigUnmarked");
        */
        aliasTable.put("unicode",               "UTF-16");
        aliasTable.put("cspc862latinhebrew",    "Cp862");
        aliasTable.put("cp-is",                 "Cp861");
        /*
        // X11 Compound Text
        aliasTable.put("x-compound-text",       "COMPOUND_TEXT");
        aliasTable.put("x11-compound_text",     "COMPOUND_TEXT");
        */
        aliasTable.put("us-ascii",              "ASCII");
        aliasTable.put("ascii",                 "ASCII");
        aliasTable.put("646",                   "ASCII");       // Solaris POSIX locale
        aliasTable.put("iso_646.irv:1983",      "ASCII");       // Linux POSIX locale
        aliasTable.put("ansi_x3.4-1968",        "ASCII");       // Caldera linux
        aliasTable.put("iso646-us",             "ASCII");
        aliasTable.put("default",               "ASCII");       // compatibility with obsolete "Default" converters
        aliasTable.put("ascii7",                "ASCII");       // compatibility with obsolete "Default" converters

        // Core encodings
        aliasTable.put("8859_1",                "ISO8859_1");
        aliasTable.put("iso8859_1",             "ISO8859_1");
        aliasTable.put("utf-8",                 "UTF8");
        aliasTable.put("utf8",                  "UTF8");
        aliasTable.put("utf-16le",              "UnicodeLittleUnmarked");

        // Standard encodings used on Solaris and Linux
        aliasTable.put("iso8859-1",             "ISO8859_1");
        aliasTable.put("iso8859-2",             "ISO8859_2");
        aliasTable.put("iso8859-4",             "ISO8859_4");
        aliasTable.put("iso8859-5",             "ISO8859_5");
        aliasTable.put("iso8859-6",             "ISO8859_6");
        aliasTable.put("iso8859-8",             "ISO8859_8");
        aliasTable.put("iso8859-9",             "ISO8859_9");
        aliasTable.put("iso8859-13",            "ISO8859_13");
        aliasTable.put("iso8859-15",            "ISO8859_15");
        aliasTable.put("5601",                  "EUC_KR");
        aliasTable.put("ansi-1251",             "Cp1251");
        aliasTable.put("big5",                  "Big5");
        aliasTable.put("big5hk",                "Big5_HKSCS");
        aliasTable.put("eucjp",                 "EUC_JP");
        aliasTable.put("cns11643",              "EUC_TW");
        aliasTable.put("gb2312",                "EUC_CN");
        aliasTable.put("gb18030",               "GB18030");
        aliasTable.put("gbk",                   "GBK");
        aliasTable.put("koi8-r",                "KOI8_R");
        aliasTable.put("tis620.2533",           "TIS620");

        // Windows encodings
        aliasTable.put("cp1250",                "Cp1250");
        aliasTable.put("cp1251",                "Cp1251");
        aliasTable.put("cp1252",                "Cp1252");
        aliasTable.put("cp1253",                "Cp1253");
        aliasTable.put("cp1254",                "Cp1254");
        aliasTable.put("cp1255",                "Cp1255");
        aliasTable.put("cp1256",                "Cp1256");
        aliasTable.put("cp1257",                "Cp1257");
        aliasTable.put("cp1258",                "Cp1258");
        aliasTable.put("ms874",                 "MS874");
        aliasTable.put("ms932",                 "MS932");
        aliasTable.put("ms949",                 "MS949");
        aliasTable.put("ms950",                 "MS950");
        aliasTable.put("ms1361",                "MS1361");

        // MIBenum: 4
        aliasTable.put("8859_1",            "ISO8859_1");
        aliasTable.put("iso_8859-1:1987",       "ISO8859_1");
        aliasTable.put("iso-ir-100",            "ISO8859_1");
        aliasTable.put("iso_8859-1",            "ISO8859_1");
        aliasTable.put("iso-8859-1",            "ISO8859_1");
        aliasTable.put("iso8859-1",             "ISO8859_1");
        aliasTable.put("latin1",                "ISO8859_1");
        aliasTable.put("l1",                    "ISO8859_1");
        aliasTable.put("ibm819",                "ISO8859_1");
        aliasTable.put("ibm-819",               "ISO8859_1");
        aliasTable.put("cp819",                 "ISO8859_1");
        aliasTable.put("819",                   "ISO8859_1");
        aliasTable.put("csisolatin1",           "ISO8859_1");

        // MIBenum: 5
        aliasTable.put("8859_2",            "ISO8859_2");
        aliasTable.put("iso_8859-2:1987",       "ISO8859_2");
        aliasTable.put("iso-ir-101",            "ISO8859_2");
        aliasTable.put("iso_8859-2",            "ISO8859_2");
        aliasTable.put("iso-8859-2",            "ISO8859_2");
        aliasTable.put("iso8859-2",             "ISO8859_2");
        aliasTable.put("latin2",                "ISO8859_2");
        aliasTable.put("l2",                    "ISO8859_2");
        aliasTable.put("ibm912",                "ISO8859_2");
        aliasTable.put("ibm-912",               "ISO8859_2");
        aliasTable.put("cp912",                 "ISO8859_2");
        aliasTable.put("912",                   "ISO8859_2");
        aliasTable.put("csisolatin2",           "ISO8859_2");

        // MIBenum: 6
        aliasTable.put("8859_3",            "ISO8859_3");
        aliasTable.put("iso_8859-3:1988",       "ISO8859_3");
        aliasTable.put("iso-ir-109",            "ISO8859_3");
        aliasTable.put("iso_8859-3",            "ISO8859_3");
        aliasTable.put("iso-8859-3",            "ISO8859_3");
        aliasTable.put("iso8859-3",             "ISO8859_3");
        aliasTable.put("latin3",                "ISO8859_3");
        aliasTable.put("l3",                    "ISO8859_3");
        aliasTable.put("ibm913",                "ISO8859_3");
        aliasTable.put("ibm-913",               "ISO8859_3");
        aliasTable.put("cp913",                 "ISO8859_3");
        aliasTable.put("913",                   "ISO8859_3");
        aliasTable.put("csisolatin3",           "ISO8859_3");

        // MIBenum: 7
        aliasTable.put("8859_4",            "ISO8859_4");
        aliasTable.put("iso_8859-4:1988",       "ISO8859_4");
        aliasTable.put("iso-ir-110",            "ISO8859_4");
        aliasTable.put("iso_8859-4",            "ISO8859_4");
        aliasTable.put("iso-8859-4",            "ISO8859_4");
        aliasTable.put("iso8859-4",             "ISO8859_4");
        aliasTable.put("latin4",                "ISO8859_4");
        aliasTable.put("l4",                    "ISO8859_4");
        aliasTable.put("ibm914",                "ISO8859_4");
        aliasTable.put("ibm-914",               "ISO8859_4");
        aliasTable.put("cp914",                 "ISO8859_4");
        aliasTable.put("914",                   "ISO8859_4");
        aliasTable.put("csisolatin4",           "ISO8859_4");

        // MIBenum: 8
        aliasTable.put("8859_5",            "ISO8859_5");
        aliasTable.put("iso_8859-5:1988",       "ISO8859_5");
        aliasTable.put("iso-ir-144",            "ISO8859_5");
        aliasTable.put("iso_8859-5",            "ISO8859_5");
        aliasTable.put("iso-8859-5",            "ISO8859_5");
        aliasTable.put("iso8859-5",             "ISO8859_5");
        aliasTable.put("cyrillic",              "ISO8859_5");
        aliasTable.put("csisolatincyrillic",    "ISO8859_5");
        aliasTable.put("ibm915",                "ISO8859_5");
        aliasTable.put("ibm-915",               "ISO8859_5");
        aliasTable.put("cp915",                 "ISO8859_5");
        aliasTable.put("915",                   "ISO8859_5");

        // MIBenum: 9
        aliasTable.put("8859_6",            "ISO8859_6");
        aliasTable.put("iso_8859-6:1987",       "ISO8859_6");
        aliasTable.put("iso-ir-127",            "ISO8859_6");
        aliasTable.put("iso_8859-6",            "ISO8859_6");
        aliasTable.put("iso-8859-6",            "ISO8859_6");
        aliasTable.put("iso8859-6",             "ISO8859_6");
        aliasTable.put("ecma-114",              "ISO8859_6");
        aliasTable.put("asmo-708",              "ISO8859_6");
        aliasTable.put("arabic",                "ISO8859_6");
        aliasTable.put("csisolatinarabic",      "ISO8859_6");
        aliasTable.put("ibm1089",               "ISO8859_6");
        aliasTable.put("ibm-1089",              "ISO8859_6");
        aliasTable.put("cp1089",                "ISO8859_6");
        aliasTable.put("1089",                  "ISO8859_6");

        // MIBenum: 10
        aliasTable.put("8859_7",            "ISO8859_7");
        aliasTable.put("iso_8859-7:1987",       "ISO8859_7");
        aliasTable.put("iso-ir-126",            "ISO8859_7");
        aliasTable.put("iso_8859-7",            "ISO8859_7");
        aliasTable.put("iso-8859-7",            "ISO8859_7");
        aliasTable.put("iso8859-7",             "ISO8859_7");
        aliasTable.put("elot_928",              "ISO8859_7");
        aliasTable.put("ecma-118",              "ISO8859_7");
        aliasTable.put("greek",                 "ISO8859_7");
        aliasTable.put("greek8",                "ISO8859_7");
        aliasTable.put("csisolatingreek",       "ISO8859_7");
        aliasTable.put("ibm813",                "ISO8859_7");
        aliasTable.put("ibm-813",               "ISO8859_7");
        aliasTable.put("cp813",                 "ISO8859_7");
        aliasTable.put("813",                   "ISO8859_7");
        aliasTable.put("sun_eu_greek",      "ISO8859_7");

        // MIBenum: 11
        aliasTable.put("8859_8",            "ISO8859_8");
        aliasTable.put("iso_8859-8:1988",       "ISO8859_8");
        aliasTable.put("iso-ir-138",            "ISO8859_8");
        aliasTable.put("iso_8859-8",            "ISO8859_8");
        aliasTable.put("iso-8859-8",            "ISO8859_8");
        aliasTable.put("iso8859-8",             "ISO8859_8");
        aliasTable.put("hebrew",                "ISO8859_8");
        aliasTable.put("csisolatinhebrew",      "ISO8859_8");
        aliasTable.put("ibm916",                "ISO8859_8");
        aliasTable.put("ibm-916",               "ISO8859_8");
        aliasTable.put("cp916",                 "ISO8859_8");
        aliasTable.put("916",                   "ISO8859_8");

        // MIBenum: 12
        aliasTable.put("8859_9",            "ISO8859_9");
        aliasTable.put("iso-ir-148",            "ISO8859_9");
        aliasTable.put("iso_8859-9",            "ISO8859_9");
        aliasTable.put("iso-8859-9",            "ISO8859_9");
        aliasTable.put("iso8859-9",             "ISO8859_9");
        aliasTable.put("latin5",                "ISO8859_9");
        aliasTable.put("l5",                    "ISO8859_9");
        aliasTable.put("ibm920",                "ISO8859_9");
        aliasTable.put("ibm-920",               "ISO8859_9");
        aliasTable.put("cp920",                 "ISO8859_9");
        aliasTable.put("920",                   "ISO8859_9");
        aliasTable.put("csisolatin5",           "ISO8859_9");

        // MIBenum: ???
        aliasTable.put("8859_13",               "ISO8859_13");
        aliasTable.put("iso_8859-13",           "ISO8859_13");
        aliasTable.put("iso-8859-13",           "ISO8859_13");
        aliasTable.put("iso8859-13",            "ISO8859_13");


        // MIBenum: ????
        aliasTable.put("8859_15",               "ISO8859_15");
        aliasTable.put("iso-8859-15",           "ISO8859_15");
        aliasTable.put("iso_8859-15",           "ISO8859_15");
        aliasTable.put("iso8859-15",            "ISO8859_15");
        aliasTable.put("ibm923",                "ISO8859_15");
        aliasTable.put("ibm-923",               "ISO8859_15");
        aliasTable.put("cp923",                 "ISO8859_15");
        aliasTable.put("923",                   "ISO8859_15");
        aliasTable.put("latin0",                "ISO8859_15");
        aliasTable.put("latin9",                "ISO8859_15");
        aliasTable.put("csisolatin0",           "ISO8859_15");
        aliasTable.put("csisolatin9",           "ISO8859_15");

        //For compatibility purpose
        aliasTable.put("iso8859_15_fdis",       "ISO8859_15");

        // MIBenum: 106
        aliasTable.put("utf-8",                 "UTF8");

        // Alias recommended in RFC 1641
        aliasTable.put("unicode-1-1-utf-8",     "UTF8");

        // MIBenum: 1000
        aliasTable.put("iso-10646-ucs-2",           "UnicodeBigUnmarked");

        // Per Unicode standard
        aliasTable.put("utf-16be",                  "UnicodeBigUnmarked");
        aliasTable.put("utf-16le",                  "UnicodeLittleUnmarked");
        aliasTable.put("utf-16",                    "UTF16");

        // Used by drag-and-drop subsystem
        aliasTable.put("x-utf-16be",        "UnicodeBigUnmarked");
        aliasTable.put("x-utf-16le",        "UnicodeLittleUnmarked");

        // MIBenum: ????
        aliasTable.put("ibm037",                "Cp037");
        aliasTable.put("ibm-037",               "Cp037");
        aliasTable.put("cp037",                 "Cp037");
        aliasTable.put("037",                   "Cp037");

        // MIBenum: ????
        aliasTable.put("ibm273",                "Cp273");
        aliasTable.put("ibm-273",               "Cp273");
        aliasTable.put("cp273",                 "Cp273");
        aliasTable.put("273",                   "Cp273");

        // MIBenum: ????
        aliasTable.put("ibm277",                "Cp277");
        aliasTable.put("ibm-277",               "Cp277");
        aliasTable.put("cp277",                 "Cp277");
        aliasTable.put("277",                   "Cp277");

        // MIBenum: ????
        aliasTable.put("ibm278",                "Cp278");
        aliasTable.put("ibm-278",               "Cp278");
        aliasTable.put("cp278",                 "Cp278");
        aliasTable.put("278",                   "Cp278");

        // MIBenum: ????
        aliasTable.put("ibm280",                "Cp280");
        aliasTable.put("ibm-280",               "Cp280");
        aliasTable.put("cp280",                 "Cp280");
        aliasTable.put("280",                   "Cp280");

        // MIBenum: ????
        aliasTable.put("ibm284",                "Cp284");
        aliasTable.put("ibm-284",               "Cp284");
        aliasTable.put("cp284",                 "Cp284");
        aliasTable.put("284",                   "Cp284");

        // MIBenum: ????
        aliasTable.put("ibm285",                "Cp285");
        aliasTable.put("ibm-285",               "Cp285");
        aliasTable.put("cp285",                 "Cp285");
        aliasTable.put("285",                   "Cp285");

        // MIBenum: ????
        aliasTable.put("ibm297",                "Cp297");
        aliasTable.put("ibm-297",               "Cp297");
        aliasTable.put("cp297",                 "Cp297");
        aliasTable.put("297",                   "Cp297");

        // MIBenum: ????
        aliasTable.put("ibm420",                "Cp420");
        aliasTable.put("ibm-420",               "Cp420");
        aliasTable.put("cp420",                 "Cp420");
        aliasTable.put("420",                   "Cp420");

        // MIBenum: ????
        aliasTable.put("ibm424",                "Cp424");
        aliasTable.put("ibm-424",               "Cp424");
        aliasTable.put("cp424",                 "Cp424");
        aliasTable.put("424",                   "Cp424");

        // MIBenum: 2011
        aliasTable.put("ibm437",                "Cp437");
        aliasTable.put("ibm-437",               "Cp437");
        aliasTable.put("cp437",                 "Cp437");
        aliasTable.put("437",                   "Cp437");
        aliasTable.put("cspc8codepage437",      "Cp437");

        // MIBenum: ????
        aliasTable.put("ibm500",                "Cp500");
        aliasTable.put("ibm-500",               "Cp500");
        aliasTable.put("cp500",                 "Cp500");
        aliasTable.put("500",                   "Cp500");

        // MIBenum: ????
        aliasTable.put("ibm737",                "Cp737");
        aliasTable.put("ibm-737",               "Cp737");
        aliasTable.put("cp737",                 "Cp737");
        aliasTable.put("737",                   "Cp737");

        // MIBenum: ????
        aliasTable.put("ibm775",                "Cp775");
        aliasTable.put("ibm-775",               "Cp775");
        aliasTable.put("cp775",                 "Cp775");
        aliasTable.put("775",                   "Cp775");

        // MIBenum: ????
        aliasTable.put("ibm838",                "Cp838");         /* MDA */
        aliasTable.put("ibm-838",               "Cp838");         /* MDA */
        aliasTable.put("cp838",                 "Cp838");         /* MDA */
        aliasTable.put("838",                   "Cp838");         /* MDA */

        // "Cp850"
        // MIBenum: 2009
        aliasTable.put("ibm850",                "Cp850");
        aliasTable.put("ibm-850",               "Cp850");
        aliasTable.put("cp850",                 "Cp850");
        aliasTable.put("850",                   "Cp850");
        aliasTable.put("cspc850multilingual",   "Cp850");

        // "Cp852"
        // MIBenum: 2010
        aliasTable.put("ibm852",                "Cp852");
        aliasTable.put("ibm-852",               "Cp852");
        aliasTable.put("cp852",                 "Cp852");
        aliasTable.put("852",                   "Cp852");
        aliasTable.put("cspcp852",              "Cp852");

        // "Cp855"
        // MIBenum: 2046
        aliasTable.put("ibm855",                "Cp855");
        aliasTable.put("ibm-855",               "Cp855");
        aliasTable.put("cp855",                 "Cp855");
        aliasTable.put("855",                   "Cp855");
        aliasTable.put("cspcp855",              "Cp855");

        // "Cp855"
        // MIBenum: ???
        aliasTable.put("ibm856",                "Cp856");
        aliasTable.put("ibm-856",               "Cp856");
        aliasTable.put("cp856",                 "Cp856");
        aliasTable.put("856",                   "Cp856");

        // "Cp857"
        // MIBenum: 2047
        aliasTable.put("ibm857",                "Cp857");
        aliasTable.put("ibm-857",               "Cp857");
        aliasTable.put("cp857",                 "Cp857");
        aliasTable.put("857",                   "Cp857");
        aliasTable.put("csibm857",              "Cp857");

        // "Cp860"
        // MIBenum: 2048
        aliasTable.put("ibm860",                "Cp860");
        aliasTable.put("ibm-860",               "Cp860");
        aliasTable.put("cp860",                 "Cp860");
        aliasTable.put("860",                   "Cp860");
        aliasTable.put("csibm860",              "Cp860");

        // MIBenum: 2049
        aliasTable.put("ibm861",                "Cp861");
        aliasTable.put("ibm-861",               "Cp861");
        aliasTable.put("cp861",                 "Cp861");
        aliasTable.put("861",                   "Cp861");
        aliasTable.put("csibm861",              "Cp861");

        // MIBenum: 2013
        aliasTable.put("ibm862",                "Cp862");
        aliasTable.put("ibm-862",               "Cp862");
        aliasTable.put("cp862",                 "Cp862");
        aliasTable.put("862",                   "Cp862");

        // MIBenum: 2050
        aliasTable.put("ibm863",                "Cp863");
        aliasTable.put("ibm-863",               "Cp863");
        aliasTable.put("cp863",                 "Cp863");
        aliasTable.put("863",                   "Cp863");
        aliasTable.put("csibm863",              "Cp863");

        // MIBenum: 2051
        aliasTable.put("ibm864",                "Cp864");
        aliasTable.put("ibm-864",               "Cp864");
        aliasTable.put("cp864",                 "Cp864");
        aliasTable.put("csibm864",              "Cp864");

        // MIBenum: 2052
        aliasTable.put("ibm865",                "Cp865");
        aliasTable.put("ibm-865",               "Cp865");
        aliasTable.put("cp865",                 "Cp865");
        aliasTable.put("865",                   "Cp865");
        aliasTable.put("csibm865",              "Cp865");

        // MIBenum: ????
        aliasTable.put("ibm866",                "Cp866");
        aliasTable.put("ibm-866",               "Cp866");
        aliasTable.put("cp866",                 "Cp866");
        aliasTable.put("866",                   "Cp866");
        aliasTable.put("csibm866",              "Cp866");

        // MIBenum: ????
        aliasTable.put("ibm868",                "Cp868");
        aliasTable.put("ibm-868",               "Cp868");
        aliasTable.put("cp868",                 "Cp868");
        aliasTable.put("868",                   "Cp868");

        // MIBenum: 2054
        aliasTable.put("ibm869",                "Cp869");
        aliasTable.put("ibm-869",               "Cp869");
        aliasTable.put("cp869",                 "Cp869");
        aliasTable.put("869",                   "Cp869");
        aliasTable.put("cp-gr",                 "Cp869");
        aliasTable.put("csibm869",              "Cp869");

        // MIBenum: ????
        aliasTable.put("ibm870",                "Cp870");
        aliasTable.put("ibm-870",               "Cp870");
        aliasTable.put("cp870",                 "Cp870");
        aliasTable.put("870",                   "Cp870");

        // MIBenum: ????
        aliasTable.put("ibm871",                "Cp871");
        aliasTable.put("ibm-871",               "Cp871");
        aliasTable.put("cp871",                 "Cp871");
        aliasTable.put("871",                   "Cp871");

        // MIBenum: ????
        aliasTable.put("ibm874",                "Cp874");
        aliasTable.put("ibm-874",               "Cp874");
        aliasTable.put("cp874",                 "Cp874");
        aliasTable.put("874",                   "Cp874");

        // MIBenum: ????
        aliasTable.put("ibm875",                "Cp875");
        aliasTable.put("ibm-875",               "Cp875");
        aliasTable.put("cp875",                 "Cp875");
        aliasTable.put("875",                   "Cp875");

        // MIBenum: ????
        aliasTable.put("ibm918",                "Cp918");
        aliasTable.put("ibm-918",               "Cp918");
        aliasTable.put("cp918",                 "Cp918");
        aliasTable.put("918",                   "Cp918");

        // MIBenum: ????
        aliasTable.put("ibm921",                "Cp921");
        aliasTable.put("ibm-921",               "Cp921");
        aliasTable.put("cp921",                 "Cp921");
        aliasTable.put("921",                   "Cp921");

        // MIBenum: ????
        aliasTable.put("ibm922",                "Cp922");
        aliasTable.put("ibm-922",               "Cp922");
        aliasTable.put("cp922",                 "Cp922");
        aliasTable.put("922",                   "Cp922");

        // MIBenum: ????
        aliasTable.put("ibm930",                "Cp930");         /* MDA */
        aliasTable.put("ibm-930",               "Cp930");         /* MDA */
        aliasTable.put("cp930",                 "Cp930");         /* MDA */
        aliasTable.put("930",                   "Cp930");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm933",                "Cp933");         /* MDA */
        aliasTable.put("ibm-933",               "Cp933");         /* MDA */
        aliasTable.put("cp933",                 "Cp933");         /* MDA */
        aliasTable.put("933",                   "Cp933");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm935",                "Cp935");         /* MDA */
        aliasTable.put("ibm-935",               "Cp935");         /* MDA */
        aliasTable.put("cp935",                 "Cp935");         /* MDA */
        aliasTable.put("935",                   "Cp935");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm937",                "Cp937");         /* MDA */
        aliasTable.put("ibm-937",               "Cp937");         /* MDA */
        aliasTable.put("cp937",                 "Cp937");         /* MDA */
        aliasTable.put("937",                   "Cp937");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm939",                "Cp939");         /* MDA */
        aliasTable.put("ibm-939",               "Cp939");         /* MDA */
        aliasTable.put("cp939",                 "Cp939");         /* MDA */
        aliasTable.put("939",                   "Cp939");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm942",                "Cp942");         /* MDA */
        aliasTable.put("ibm-942",               "Cp942");         /* MDA */
        aliasTable.put("cp942",                 "Cp942");         /* MDA */
        aliasTable.put("942",                   "Cp942");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm943",                "Cp943");         /* ibm.3158 */
        aliasTable.put("ibm-943",               "Cp943");         /* ibm.3158 */
        aliasTable.put("cp943",                 "Cp943");         /* ibm.3158 */
        aliasTable.put("943",                   "Cp943");         /* ibm.3158 */

        // MIBenum: ????
        aliasTable.put("ibm948",                "Cp948");         /* MDA */
        aliasTable.put("ibm-948",               "Cp948");         /* MDA */
        aliasTable.put("cp948",                 "Cp948");         /* MDA */
        aliasTable.put("948",                   "Cp948");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm949",                "Cp949");         /* MDA */
        aliasTable.put("ibm-949",               "Cp949");         /* MDA */
        aliasTable.put("cp949",                 "Cp949");         /* MDA */
        aliasTable.put("949",                   "Cp949");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm950",                "Cp950");         /* MDA */
        aliasTable.put("ibm-950",               "Cp950");         /* MDA */
        aliasTable.put("cp950",                 "Cp950");         /* MDA */
        aliasTable.put("950",                   "Cp950");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm964",                "Cp964");         /* MDA */
        aliasTable.put("ibm-964",               "Cp964");         /* MDA */
        aliasTable.put("cp964",                 "Cp964");         /* MDA */
        aliasTable.put("964",                   "Cp964");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm970",                "Cp970");         /* MDA */
        aliasTable.put("ibm-970",               "Cp970");         /* MDA */
        aliasTable.put("cp970",                 "Cp970");         /* MDA */
        aliasTable.put("970",                   "Cp970");         /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm1006",               "Cp1006");
        aliasTable.put("ibm-1006",              "Cp1006");
        aliasTable.put("cp1006",                "Cp1006");
        aliasTable.put("1006",                  "Cp1006");

        // MIBenum: ????
        aliasTable.put("ibm1025",               "Cp1025");
        aliasTable.put("ibm-1025",              "Cp1025");
        aliasTable.put("cp1025",                "Cp1025");
        aliasTable.put("1025",                  "Cp1025");

        // MIBenum: ????
        aliasTable.put("ibm1026",               "Cp1026");
        aliasTable.put("ibm-1026",              "Cp1026");
        aliasTable.put("cp1026",                "Cp1026");
        aliasTable.put("1026",                  "Cp1026");

        // MIBenum: ????
        aliasTable.put("ibm1097",               "Cp1097");
        aliasTable.put("ibm-1097",              "Cp1097");
        aliasTable.put("cp1097",                "Cp1097");
        aliasTable.put("1097",                  "Cp1097");

        // MIBenum: ????
        aliasTable.put("ibm1098",               "Cp1098");
        aliasTable.put("ibm-1098",              "Cp1098");
        aliasTable.put("cp1098",                "Cp1098");
        aliasTable.put("1098",                  "Cp1098");

        // MIBenum: ????
        aliasTable.put("ibm1112",               "Cp1112");
        aliasTable.put("ibm-1112",              "Cp1112");
        aliasTable.put("cp1112",                "Cp1112");
        aliasTable.put("1112",                  "Cp1112");

        // MIBenum: ????
        aliasTable.put("ibm1122",               "Cp1122");
        aliasTable.put("ibm-1122",              "Cp1122");
        aliasTable.put("cp1122",                "Cp1122");
        aliasTable.put("1122",                  "Cp1122");

        // MIBenum: ????
        aliasTable.put("ibm1123",               "Cp1123");
        aliasTable.put("ibm-1123",              "Cp1123");
        aliasTable.put("cp1123",                "Cp1123");
        aliasTable.put("1123",                  "Cp1123");

        // MIBenum: ????
        aliasTable.put("ibm1124",               "Cp1124");
        aliasTable.put("ibm-1124",              "Cp1124");
        aliasTable.put("cp1124",                "Cp1124");
        aliasTable.put("1124",                  "Cp1124");

        // MIBenum: ????
        aliasTable.put("ibm1129",               "Cp1129");
        aliasTable.put("ibm-1129",              "Cp1129");
        aliasTable.put("cp1129",                "Cp1129");
        aliasTable.put("1129",                  "Cp1129");

        // MIBenum: ????
        aliasTable.put("ibm1166",               "Cp1166");
        aliasTable.put("ibm-1166",              "Cp1166");
        aliasTable.put("cp1166",                "Cp1166");
        aliasTable.put("1166",                  "Cp1166");

        // MIBenum: ????
        aliasTable.put("ibm1381",               "Cp1381");        /* MDA */
        aliasTable.put("ibm-1381",              "Cp1381");        /* MDA */
        aliasTable.put("cp1381",                "Cp1381");        /* MDA */
        aliasTable.put("1381",                  "Cp1381");        /* MDA */

        // MIBenum: ????
        aliasTable.put("ibm1383",               "Cp1383");        /* MDA */
        aliasTable.put("ibm-1383",              "Cp1383");        /* MDA */
        aliasTable.put("cp1383",                "Cp1383");        /* MDA */
        aliasTable.put("1383",                  "Cp1383");        /* MDA */

        // MIBenum: 16/39
        aliasTable.put("jis",               "ISO2022JP");
        aliasTable.put("iso-2022-jp",           "ISO2022JP");
        aliasTable.put("csiso2022jp",           "ISO2022JP");
        aliasTable.put("jis_encoding",          "ISO2022JP");
        aliasTable.put("csjisencoding",         "ISO2022JP");

        // MIBenum: 17/2024
        aliasTable.put("windows-31j",           "MS932");
        aliasTable.put("cswindows31j",          "MS932");


        aliasTable.put("pck", "PCK");       // Case independent PCK alias

        /*if (sjisIsMS932) {
        aliasTable.put("shift_jis",         "MS932");   // IANA shift jis aliases
        aliasTable.put("csshiftjis",        "MS932");   // updated per 4556882
        aliasTable.put("x-sjis",            "MS932");
        aliasTable.put("ms_kanji",          "MS932");
        } else {
        */
            aliasTable.put("shift_jis",         "SJIS");        // IANA shift jis aliases
            aliasTable.put("csshiftjis",        "SJIS");
            aliasTable.put("x-sjis",            "SJIS");
            aliasTable.put("ms_kanji",          "SJIS");
            /*
        }
            */
        // MIBenum: 18
        // Japanese EUC
        aliasTable.put("eucjis",                    "EUC_JP");
        aliasTable.put("euc-jp",                    "EUC_JP");
        aliasTable.put("eucjp",             "EUC_JP");
        aliasTable.put("extended_unix_code_packed_format_for_japanese",
                       "EUC_JP");
        aliasTable.put("cseucpkdfmtjapanese",   "EUC_JP");
        aliasTable.put("x-euc-jp",          "EUC_JP");
        aliasTable.put("x-eucjp",           "EUC_JP");
            aliasTable.put("eucjp-open",            "EUC_JP_Solaris"); // 1.3.1_x compatibility

        // For handing only JIS0202 and JIS0208 in linux
        aliasTable.put("euc-jp-linux",          "EUC_JP_LINUX");

        // MIBenum: 874
        aliasTable.put("windows-874",           "MS874");

        // MIBenum: 2250
        aliasTable.put("windows-1250",          "Cp1250");

        // MIBenum: 2251
        aliasTable.put("windows-1251",          "Cp1251");
        aliasTable.put("ansi-1251",             "Cp1251"); // Solaris ru_RU.ANSI1251 locale

        // MIBenum: 2252
        aliasTable.put("windows-1252",          "Cp1252");

        // MIBenum: 2253
        aliasTable.put("windows-1253",          "Cp1253");

        // MIBenum: 2254
        aliasTable.put("windows-1254",          "Cp1254");

        // MIBenum: 2255
        aliasTable.put("windows-1255",          "Cp1255");

        // MIBenum: 2256
        aliasTable.put("windows-1256",          "Cp1256");

        // MIBenum: 2257
        aliasTable.put("windows-1257",          "Cp1257");

        // MIBenum: 2258
        aliasTable.put("windows-1258",          "Cp1258");

        // MIBenum: ????
        aliasTable.put("ibm33722",              "Cp33722");       /* MDA */
        aliasTable.put("ibm-33722",             "Cp33722");       /* MDA */
        aliasTable.put("cp33722",               "Cp33722");       /* MDA */
        aliasTable.put("33722",                 "Cp33722");       /* MDA */

        // Russian KOI8-R
        aliasTable.put("koi8-r",                "KOI8_R");
        aliasTable.put("koi8",                  "KOI8_R");
        aliasTable.put("cskoi8r",               "KOI8_R");

        // Simplified Chinese
        aliasTable.put("gb2312",                    "EUC_CN");
        aliasTable.put("gb2312-80",                 "EUC_CN");
        aliasTable.put("gb2312-1980",           "EUC_CN");
        aliasTable.put("euc-cn",                    "EUC_CN");
        aliasTable.put("euccn",             "EUC_CN");

        aliasTable.put("big5",              "Big5");
        aliasTable.put("big5hk",                    "Big5_HKSCS");
        aliasTable.put("big5-hkscs",        "Big5_HKSCS");
        // Added for future compatibility, explicit mapping to Unicode 3.0
        aliasTable.put("big5-hkscs:unicode3.0", "Big5_HKSCS");
        aliasTable.put("big5_solaris",      "Big5_Solaris");

        // Traditional Chinese
        aliasTable.put("cns11643",                  "EUC_TW");
        aliasTable.put("euc-tw",                    "EUC_TW");
        aliasTable.put("euctw",             "EUC_TW");

        // Korean
        aliasTable.put("ksc5601",               "EUC_KR");
        aliasTable.put("euc-kr",                "EUC_KR");
        aliasTable.put("euckr",                 "EUC_KR");
        aliasTable.put("ks_c_5601-1987",        "EUC_KR");
        aliasTable.put("ksc5601-1987",          "EUC_KR");
        aliasTable.put("ksc5601_1987",          "EUC_KR");
        aliasTable.put("ksc_5601",              "EUC_KR");
        aliasTable.put("5601",                  "EUC_KR");

        aliasTable.put("ksc5601-1992",          "Johab");
        aliasTable.put("ksc5601_1992",          "Johab");
        aliasTable.put("ms1361",                "Johab");

        aliasTable.put("windows-949",           "MS949");

        //MIBenum: 37
        aliasTable.put("iso-2022-kr",           "ISO2022KR");
        aliasTable.put("csiso2022kr",           "ISO2022KR");

        // Thai
        aliasTable.put("tis620.2533",           "TIS620");
        aliasTable.put("tis-620",               "TIS620"); // Linux name

        // Variants
        aliasTable.put("cp942c", "Cp942C");
        aliasTable.put("cp943c", "Cp943C");
        aliasTable.put("cp949c", "Cp949C");
        aliasTable.put("iscii", "ISCII91");
    }
}
