/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4513767 4961027 6217210 8242541
 * @summary Checks canonical names match between old and (NIO) core charsets
 * @modules jdk.charsets
 */
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public class CheckHistoricalNames {
    static int failed = 0;
    public static void main (String[] args) throws Exception {
        checkHistoricalName("ASCII");
        checkHistoricalName("Cp1252");
        checkHistoricalName("ISO8859_1");
        checkHistoricalName("UnicodeBigUnmarked");
        checkHistoricalName("UnicodeLittle");
        checkHistoricalName("UnicodeLittleUnmarked");
        checkHistoricalName("UTF8");
        checkHistoricalName("UTF-16");

        checkMappedName("UnicodeBig", "UTF-16");
        checkMappedName("US-ASCII", "ASCII");
        checkMappedName("ISO-8859-1", "ISO8859_1");
        checkMappedName("UTF-8", "UTF8");
        checkMappedName("UTF-16BE", "UnicodeBigUnmarked");
        checkMappedName("UTF-16LE", "UnicodeLittleUnmarked");

        checkHistoricalName("ISO8859_2");
        checkHistoricalName("ISO8859_4");
        checkHistoricalName("ISO8859_5");
        checkHistoricalName("ISO8859_7");
        checkHistoricalName("ISO8859_9");
        checkHistoricalName("ISO8859_13");
        checkHistoricalName("KOI8_R");
        checkHistoricalName("Cp1250");
        checkHistoricalName("Cp1251");
        checkHistoricalName("Cp1253");
        checkHistoricalName("Cp1254");
        checkHistoricalName("Cp1257");

        checkMappedName("ISO-8859-2", "ISO8859_2");
        checkMappedName("ISO-8859-4", "ISO8859_4");
        checkMappedName("ISO-8859-5", "ISO8859_5");
        checkMappedName("ISO-8859-7", "ISO8859_7");
        checkMappedName("ISO-8859-9", "ISO8859_9");
        checkMappedName("ISO-8859-13", "ISO8859_13");
        checkMappedName("KOI8-R", "KOI8_R");
        checkMappedName("windows-1250", "Cp1250");
        checkMappedName("windows-1251","Cp1251");
        checkMappedName("windows-1253", "Cp1253");
        checkMappedName("windows-1254", "Cp1254");
        checkMappedName("windows-1257", "Cp1257");

        checkHistoricalName("EUC_CN");
        checkHistoricalName("EUC_JP");
        checkHistoricalName("EUC_JP_LINUX");
        checkHistoricalName("EUC_KR");
        checkHistoricalName("EUC_TW");
        checkHistoricalName("ISO2022CN");
        checkHistoricalName("ISO2022JP");
        checkHistoricalName("ISO2022KR");
        checkHistoricalName("ISO8859_3");
        checkHistoricalName("ISO8859_6");
        checkHistoricalName("ISO8859_8");
        checkHistoricalName("Cp1255");
        checkHistoricalName("Cp1256");
        checkHistoricalName("Cp1258");
        checkHistoricalName("MS936");
        checkHistoricalName("MS949");
        checkHistoricalName("MS950");
        checkHistoricalName("TIS620");

        checkMappedName("EUC-CN", "EUC_CN");
        checkMappedName("EUC-JP", "EUC_JP");
        checkMappedName("EUC-JP-LINUX", "EUC_JP_LINUX");
        checkMappedName("EUC-TW", "EUC_TW");
        checkMappedName("EUC-KR", "EUC_KR");
        checkMappedName("ISO-2022-CN", "ISO2022CN");
        checkMappedName("ISO-2022-JP", "ISO2022JP");
        checkMappedName("ISO-2022-KR", "ISO2022KR");
        checkMappedName("ISO-8859-3", "ISO8859_3");
        checkMappedName("ISO-8859-6", "ISO8859_6");
        checkMappedName("ISO-8859-8", "ISO8859_8");
        checkMappedName("windows-1255", "Cp1255");
        checkMappedName("windows-1256", "Cp1256");
        checkMappedName("windows-1258", "Cp1258");
        checkMappedName("windows-936", "GBK");
        checkMappedName("windows-949", "MS949");
        checkMappedName("windows-950", "MS950");
        checkMappedName("x-MS950-HKSCS", "MS950_HKSCS");
        checkMappedName("x-PCK", "PCK");
        checkMappedName("Shift_JIS", "SJIS");
        checkMappedName("x-JISAutoDetect", "JISAutoDetect");
        checkMappedName("TIS-620", "TIS620");
        checkMappedName("x-Big5-Solaris", "Big5_Solaris");

        checkHistoricalName("Cp037");
        checkHistoricalName("Cp1006");
        checkHistoricalName("Cp1025");
        checkHistoricalName("Cp1026");
        checkHistoricalName("Cp1046");
        checkHistoricalName("Cp1047");
        checkHistoricalName("Cp1097");
        checkHistoricalName("Cp1098");
        checkHistoricalName("Cp1112");
        checkHistoricalName("Cp1122");
        checkHistoricalName("Cp1123");
        checkHistoricalName("Cp1124");
        checkHistoricalName("Cp1140");
        checkHistoricalName("Cp1141");
        checkHistoricalName("Cp1142");
        checkHistoricalName("Cp1143");
        checkHistoricalName("Cp1144");
        checkHistoricalName("Cp1145");
        checkHistoricalName("Cp1146");
        checkHistoricalName("Cp1147");
        checkHistoricalName("Cp1148");
        checkHistoricalName("Cp1149");
        checkHistoricalName("Cp1381");
        checkHistoricalName("Cp1383");
        checkHistoricalName("Cp273");
        checkHistoricalName("Cp277");
        checkHistoricalName("Cp278");
        checkHistoricalName("Cp280");
        checkHistoricalName("Cp284");
        checkHistoricalName("Cp285");
        checkHistoricalName("Cp297");
        checkHistoricalName("Cp33722");
        checkHistoricalName("Cp420");
        checkHistoricalName("Cp424");
        checkHistoricalName("Cp437");
        checkHistoricalName("Cp500");
        checkHistoricalName("Cp737");
        checkHistoricalName("Cp775");
        checkHistoricalName("Cp833");
        checkHistoricalName("Cp838");
        checkHistoricalName("Cp850");
        checkHistoricalName("Cp852");
        checkHistoricalName("Cp855");
        checkHistoricalName("Cp856");
        checkHistoricalName("Cp857");
        checkHistoricalName("Cp858");
        checkHistoricalName("Cp860");
        checkHistoricalName("Cp861");
        checkHistoricalName("Cp862");
        checkHistoricalName("Cp863");
        checkHistoricalName("Cp864");
        checkHistoricalName("Cp865");
        checkHistoricalName("Cp866");
        checkHistoricalName("Cp868");
        checkHistoricalName("Cp869");
        checkHistoricalName("Cp870");
        checkHistoricalName("Cp871");
        checkHistoricalName("Cp874");
        checkHistoricalName("Cp875");
        checkHistoricalName("Cp918");
        checkHistoricalName("Cp921");
        checkHistoricalName("Cp922");
        checkHistoricalName("Cp933");
        checkHistoricalName("Cp939");
        checkHistoricalName("Cp949");
        checkHistoricalName("Cp964");
        checkHistoricalName("Cp970");

        checkMappedName("IBM037", "Cp037");
        checkMappedName("IBM1006", "Cp1006");
        checkMappedName("IBM1025", "Cp1025");
        checkMappedName("IBM1026", "Cp1026");
        checkMappedName("x-IBM1046", "Cp1046");
        checkMappedName("IBM1047", "Cp1047");
        checkMappedName("IBM1097", "Cp1097");
        checkMappedName("IBM1098", "Cp1098");
        checkMappedName("IBM1112", "Cp1112");
        checkMappedName("IBM1122", "Cp1122");
        checkMappedName("IBM1123", "Cp1123");
        checkMappedName("IBM1124", "Cp1124");
        checkMappedName("IBM1129", "Cp1129");
        checkMappedName("IBM1166", "Cp1166");
        checkMappedName("IBM01140", "Cp1140");
        checkMappedName("IBM01141", "Cp1141");
        checkMappedName("IBM01142", "Cp1142");
        checkMappedName("IBM01143", "Cp1143");
        checkMappedName("IBM01144", "Cp1144");
        checkMappedName("IBM01145", "Cp1145");
        checkMappedName("IBM01146", "Cp1146");
        checkMappedName("IBM01147", "Cp1147");
        checkMappedName("IBM01148", "Cp1148");
        checkMappedName("IBM01149", "Cp1149");
        checkMappedName("IBM1381", "Cp1381");
        checkMappedName("IBM1383", "Cp1383");
        checkMappedName("IBM273", "Cp273");
        checkMappedName("IBM277", "Cp277");
        checkMappedName("IBM278", "Cp278");
        checkMappedName("IBM280", "Cp280");
        checkMappedName("IBM284", "Cp284");
        checkMappedName("IBM285", "Cp285");
        checkMappedName("IBM297", "Cp297");
        checkMappedName("IBM33722", "Cp33722");
        checkMappedName("IBM420", "Cp420");
        checkMappedName("IBM424", "Cp424");
        checkMappedName("IBM437", "Cp437");
        checkMappedName("IBM500", "Cp500");
        checkMappedName("IBM737", "Cp737");
        checkMappedName("IBM775", "Cp775");
        checkMappedName("IBM838", "Cp838");
        checkMappedName("IBM850", "Cp850");
        checkMappedName("IBM852", "Cp852");
        checkMappedName("IBM855", "Cp855");
        checkMappedName("IBM856", "Cp856");
        checkMappedName("IBM857", "Cp857");
        checkMappedName("IBM00858", "Cp858");
        checkMappedName("IBM833", "Cp833");
        checkMappedName("IBM860", "Cp860");
        checkMappedName("IBM861", "Cp861");
        checkMappedName("IBM862", "Cp862");
        checkMappedName("IBM863", "Cp863");
        checkMappedName("IBM864", "Cp864");
        checkMappedName("IBM865", "Cp865");
        checkMappedName("IBM866", "Cp866");
        checkMappedName("IBM868", "Cp868");
        checkMappedName("IBM869", "Cp869");
        checkMappedName("IBM870", "Cp870");
        checkMappedName("IBM871", "Cp871");
        checkMappedName("IBM874", "Cp874");
        checkMappedName("IBM875", "Cp875");
        checkMappedName("IBM918", "Cp918");
        checkMappedName("IBM921", "Cp921");
        checkMappedName("IBM922", "Cp922");
        checkMappedName("x-IBM930", "Cp930");
        checkMappedName("IBM933", "Cp933");
        checkMappedName("x-IBM935", "Cp935");
        checkMappedName("x-IBM937", "Cp937");
        checkMappedName("IBM939", "Cp939");
        checkMappedName("x-IBM942", "Cp942");
        checkMappedName("x-IBM942C", "Cp942C");
        checkMappedName("x-IBM943", "Cp943");
        checkMappedName("x-IBM943C", "Cp943C");
        checkMappedName("x-IBM948", "Cp948");
        checkMappedName("IBM949", "Cp949");
        checkMappedName("x-IBM949C", "Cp949C");
        checkMappedName("x-IBM950", "Cp950");
        checkMappedName("IBM964", "Cp964");
        checkMappedName("IBM970", "Cp970");

        checkHistoricalName("MacArabic");
        checkHistoricalName("MacCentralEurope");
        checkHistoricalName("MacCroatian");
        checkHistoricalName("MacCyrillic");
        checkHistoricalName("MacDingbat");
        checkHistoricalName("MacGreek");
        checkHistoricalName("MacHebrew");
        checkHistoricalName("MacIceland");
        checkHistoricalName("MacRoman");
        checkHistoricalName("MacRomania");
        checkHistoricalName("MacSymbol");
        checkHistoricalName("MacThai");
        checkHistoricalName("MacTurkish");
        checkHistoricalName("MacUkraine");

        checkMappedName("x-MacArabic", "MacArabic");
        checkMappedName("x-MacCentralEurope", "MacCentralEurope");
        checkMappedName("x-MacCroatian", "MacCroatian");
        checkMappedName("x-MacCyrillic", "MacCyrillic");
        checkMappedName("x-MacDingbat", "MacDingbat");
        checkMappedName("x-MacGreek", "MacGreek");
        checkMappedName("x-MacHebrew", "MacHebrew");
        checkMappedName("x-MacIceland", "MacIceland");
        checkMappedName("x-MacRoman", "MacRoman");
        checkMappedName("x-MacRomania", "MacRomania");
        checkMappedName("x-MacSymbol", "MacSymbol");
        checkMappedName("x-MacThai", "MacThai");
        checkMappedName("x-MacTurkish", "MacTurkish");
        checkMappedName("x-MacUkraine", "MacUkraine");
        checkCharsetAndHistoricalName();

        if (failed != 0)
            throw new Exception("Test Failed: " + failed);
        else
            System.out.println("Test Passed!");
    }

    private static void checkHistoricalName(String name) throws Exception {
        checkMappedName(name, name);
    }

    private static void checkMappedName(String alias, String canonical)
        throws Exception {
        InputStreamReader reader = new InputStreamReader(System.in, alias);
        if (!reader.getEncoding().equals(canonical)) {
            System.out.println("Failed canonical names : mismatch for " + alias
                               + " - expected " + canonical
                               + ", got " + reader.getEncoding());
            failed++;
        }
    }

    private static void checkCharsetAndHistoricalName() {
        for (Charset cs : Charset.availableCharsets().values()) {
            InputStreamReader isr = new InputStreamReader(System.in, cs);
            String encoding = isr.getEncoding();
            try {
                Charset csHist = Charset.forName(encoding);
                if (!cs.equals(csHist)) {
                    System.out.println("Failed charset name"
                                       + " - expected " + cs.name()
                                       + ", got " + csHist.name());
                    failed++;
                }
            } catch (UnsupportedCharsetException uce) {
                System.out.println("Failed : charset - " + cs.name()
                                   + ", missing alias entry - " + encoding);
                failed++;
            }
        }
    }
}
