/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4625418 8239965
 * @summary Tests XML <a href="http://download.java.net/jdk6/docs/technotes/guides/intl/encoding.doc.html">encoding</a>
 * @author Sergey Malenkov
 * @run main/timeout=360 Test4625418
 */

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

public final class Test4625418 implements ExceptionListener {

    private static final String[] encodings = {
        "ASCII",
        "Big5",
        //"Big5-HKSCS",
        //"Big5_HKSCS",
        "Big5_Solaris",
        //"Cp037",
        "Cp1006",
        //"Cp1025",
        //"Cp1026",
        "Cp1046",
        "Cp1047",
        "Cp1097",
        "Cp1098",
        //"Cp1112",
        //"Cp1122",
        //"Cp1123",
        "Cp1124",
        //"Cp1140",
        //"Cp1141",
        //"Cp1142",
        //"Cp1143",
        //"Cp1144",
        //"Cp1145",
        //"Cp1146",
        //"Cp1147",
        //"Cp1148",
        //"Cp1149",
        "Cp1250",
        "Cp1251",
        "Cp1252",
        "Cp1253",
        "Cp1254",
        "Cp1255",
        "Cp1256",
        "Cp1257",
        "Cp1258",
        //"Cp1381",
        //"Cp1383",
        //"Cp273",
        //"Cp277",
        //"Cp278",
        //"Cp280",
        //"Cp284",
        //"Cp285",
        //"Cp297",
        //"Cp33722",
        //"Cp420",
        //"Cp424",
        "Cp437",
        //"Cp500",
        //"Cp50220",
        //"Cp50221",
        "Cp737",
        "Cp775",
        //"Cp834",
        //"Cp838",
        "Cp850",
        "Cp852",
        "Cp855",
        "Cp856",
        "Cp857",
        "Cp858",
        "Cp860",
        "Cp861",
        "Cp862",
        "Cp863",
        "Cp864",
        "Cp865",
        "Cp866",
        "Cp868",
        "Cp869",
        //"Cp870",
        //"Cp871",
        "Cp874",
        //"Cp875",
        //"Cp918",
        "Cp921",
        "Cp922",
        //"Cp930",
        "Cp933",
        //"Cp935",
        //"Cp937",
        //"Cp939",
        //"Cp942",
        //"Cp942C",
        //"Cp943",
        //"Cp943C",
        "Cp948",
        "Cp949",
        //"Cp949C",
        "Cp950",
        "Cp964",
        //"Cp970",
        //"EUC-JP",
        "EUC-KR",
        "EUC_CN",
        //"EUC_JP",
        //"EUC_JP_LINUX",
        //"EUC_JP_Solaris",
        "EUC_KR",
        //"EUC_TW",
        "GB18030",
        "GB2312",
        "GBK",
        //"IBM-Thai",
        "IBM00858",
        //"IBM01140",
        //"IBM01141",
        //"IBM01142",
        //"IBM01143",
        //"IBM01144",
        //"IBM01145",
        //"IBM01146",
        //"IBM01147",
        //"IBM01148",
        //"IBM01149",
        //"IBM037",
        //"IBM1026",
        "IBM1047",
        //"IBM273",
        //"IBM277",
        //"IBM278",
        //"IBM280",
        //"IBM284",
        //"IBM285",
        //"IBM297",
        //"IBM420",
        //"IBM424",
        "IBM437",
        //"IBM500",
        "IBM775",
        "IBM850",
        "IBM852",
        "IBM855",
        "IBM857",
        "IBM860",
        "IBM861",
        "IBM862",
        "IBM863",
        "IBM864",
        "IBM865",
        "IBM866",
        "IBM868",
        "IBM869",
        //"IBM870",
        //"IBM871",
        //"IBM918",
        //"ISCII91",
        //"ISO-2022-CN",
        "ISO-2022-JP",
        "ISO-2022-KR",
        "ISO-8859-1",
        "ISO-8859-13",
        "ISO-8859-15",
        "ISO-8859-2",
        "ISO-8859-3",
        "ISO-8859-4",
        "ISO-8859-5",
        "ISO-8859-6",
        "ISO-8859-7",
        "ISO-8859-8",
        "ISO-8859-9",
        //"ISO2022CN",
        "ISO2022JP",
        "ISO2022KR",
        //"ISO2022_CN_CNS",
        //"ISO2022_CN_GB",
        "ISO8859_1",
        "ISO8859_13",
        "ISO8859_15",
        "ISO8859_2",
        "ISO8859_3",
        "ISO8859_4",
        "ISO8859_5",
        "ISO8859_6",
        "ISO8859_7",
        "ISO8859_8",
        "ISO8859_9",
        //"JISAutoDetect",
        //"JIS_X0201",
        //"JIS_X0212-1990",
        "KOI8-R",
        "KOI8-U",
        "KOI8_R",
        "KOI8_U",
        "MS874",
        //"MS932",
        //"MS936",
        "MS949",
        "MS950",
        //"MS950_HKSCS",
        "MacArabic",
        "MacCentralEurope",
        "MacCroatian",
        "MacCyrillic",
        //"MacDingbat",
        "MacGreek",
        "MacHebrew",
        "MacIceland",
        "MacRoman",
        "MacRomania",
        //"MacSymbol",
        "MacThai",
        "MacTurkish",
        "MacUkraine",
        //"PCK",
        //"SJIS",
        //"Shift_JIS",
        "TIS-620",
        "TIS620",
        "US-ASCII",
        "UTF-16",
        "UTF-16BE",
        "UTF-16LE",
        "UTF-32",
        "UTF-32BE",
        "UTF-32LE",
        "UTF-8",
        "UTF8",
        "UTF_32",
        "UTF_32BE",
        //"UTF_32BE_BOM",
        "UTF_32LE",
        //"UTF_32LE_BOM",
        "UnicodeBig",
        "UnicodeBigUnmarked",
        "UnicodeLittle",
        "UnicodeLittleUnmarked",
        "windows-1250",
        "windows-1251",
        "windows-1252",
        "windows-1253",
        "windows-1254",
        "windows-1255",
        "windows-1256",
        "windows-1257",
        "windows-1258",
        //"windows-31j",
        //"x-Big5_Solaris",
        //"x-EUC-TW",
        "x-IBM1006",
        //"x-IBM1025",
        "x-IBM1046",
        "x-IBM1097",
        "x-IBM1098",
        //"x-IBM1112",
        //"x-IBM1122",
        //"x-IBM1123",
        "x-IBM1124",
        //"x-IBM1381",
        //"x-IBM1383",
        //"x-IBM33722",
        "x-IBM737",
        //"x-IBM834",
        "x-IBM856",
        "x-IBM874",
        //"x-IBM875",
        "x-IBM921",
        "x-IBM922",
        //"x-IBM930",
        "x-IBM933",
        //"x-IBM935",
        //"x-IBM937",
        //"x-IBM939",
        //"x-IBM942",
        //"x-IBM942C",
        //"x-IBM943",
        //"x-IBM943C",
        "x-IBM948",
        "x-IBM949",
        //"x-IBM949C",
        "x-IBM950",
        "x-IBM964",
        //"x-IBM970",
        //"x-ISCII91",
        //"x-ISO2022-CN-CNS",
        //"x-ISO2022-CN-GB",
        //"x-JIS0208",
        //"x-JISAutoDetect",
        "x-Johab",
        //"x-MS950-HKSCS",
        "x-MacArabic",
        "x-MacCentralEurope",
        "x-MacCroatian",
        "x-MacCyrillic",
        //"x-MacDingbat",
        "x-MacGreek",
        "x-MacHebrew",
        "x-MacIceland",
        "x-MacRoman",
        "x-MacRomania",
        //"x-MacSymbol",
        "x-MacThai",
        "x-MacTurkish",
        "x-MacUkraine",
        //"x-PCK",
        "x-UTF-16LE-BOM",
        //"x-UTF-32BE-BOM",
        //"x-UTF-32LE-BOM",
        //"x-euc-jp-linux",
        //"x-eucJP-Open",
        "x-iso-8859-11",
        "x-mswin-936",
        //"x-windows-50220",
        //"x-windows-50221",
        "x-windows-874",
        "x-windows-949",
        "x-windows-950",
        //"x-windows-iso2022jp",
    };

    public static void main(final String[] args) {
        final String string = createString(0x10000);
        for (String encoding : encodings) {
            System.out.println("Test encoding: " + encoding);
            new Test4625418(encoding).test(string);
        }
    }

    private static String createString(int length) {
        StringBuilder sb = new StringBuilder(length);
        while (0 < length--)
            sb.append((char) length);

        return sb.toString();
    }

    private final String encoding;

    private Test4625418(final String encoding) {
        this.encoding = encoding;
    }

    private void test(String string) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            XMLEncoder encoder = new XMLEncoder(output, this.encoding, true, 0);
            encoder.setExceptionListener(this);
            encoder.writeObject(string);
            encoder.close();

            InputStream input = new ByteArrayInputStream(output.toByteArray());
            XMLDecoder decoder = new XMLDecoder(input);
            decoder.setExceptionListener(this);
            Object object = decoder.readObject();
            decoder.close();

            if (!string.equals(object)) {
                throw new Error(this.encoding + " - can't read properly");
            }
        }
        catch (IllegalCharsetNameException exception) {
            throw new Error(this.encoding + " - illegal charset name", exception);
        }
        catch (UnsupportedCharsetException exception) {
            throw new Error(this.encoding + " - unsupported charset", exception);
        }
        catch (UnsupportedOperationException exception) {
            throw new Error(this.encoding + " - unsupported encoder", exception);
        }
    }

    public void exceptionThrown(Exception exception) {
        throw new Error(this.encoding + " - internal", exception);
    }
}
