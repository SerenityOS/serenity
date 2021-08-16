/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4881291 4094886
 * @summary checks for processing errors in properties.load
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class LoadParsing2 {
    public static void main(String[] argv) throws Exception {
        for (int i = 0; i < dfiles.length; i++) {
            test(dfiles[i], keys[i], values[i], true);
            test(dfiles[i], keys[i], values[i], false);
        }
    }

    private static Properties getLoadedProperties(InputStream is,
                                                  boolean doStream)
        throws Exception
    {
        Properties props = new Properties();
        if (doStream)
            props.load(is);
        else
            props.load(new InputStreamReader(is, "UTF-8"));
        return props;
    }

    private static void test(String fnData,
                             String[] keys,
                             String[] values,
                             boolean doStream)
        throws Exception
    {
        File f = new File(System.getProperty("test.src", "."), fnData);
        FileInputStream myIn = new FileInputStream(f);
        Properties myProps = getLoadedProperties(myIn, doStream);
        for (int i = 0; i < keys.length; i++) {
            if (!myProps.getProperty(keys[i]).equals(values[i])) {
                throw new RuntimeException("Test1: Bad parsing at " + i);
            }
        }
    }

    static String[] keys1 = {
        "\\",
        "\\:key12",
        "key16 b",
        "key14_asdfa",
        "\\\\",
        "key8notassign",
        "key17",
        "key15",
        "keyabcdef",
        "key13dialog.3",
        "key7",
        "key6",
        "key5",
        "key3",
        "key2",
        "key1",
        "key9 Term",
        "key0"
    };

    static String[] keys2 = {
        "key1",
        "key2"
    };

    static String[] keys3 = {
        "key1",
        "key2"
    };

    static String[] values1 = {
        "key10=bar",
        "bar2",
        " abcdef",
        "",
        "key11=bar2",
        "abcdef",
        "#barbaz",
        " abcdef",
        "",
        "",
        "Symbol,SYMBOL_CHARSET ",
        "WingDings,SYMBOL_CHARSET \\abc",
        "==Arial,ANSI_CHARSET",
        "",
        "= abcde",
        "value1",
        "ABCDE",
        "abcd"
    };

    static String[] values2 = {
        "-monotype-timesnewroman-regular-r---*-%d-*-*-p-*-iso8859-1serif.1a-monotype-timesnewroman-regular-r-normal--*-%d-*-*-p-*-iso8859-2serif.2a-b&h-LucidaBrightLat4-Normal-r-normal--*-%d-*-*-p-*-iso8859-4serif.3a-monotype-times-regular-r-normal--*-%d-*-*-p-*-iso8859-5serif.4a-monotype-timesnewromangreek-regular-r-normal--*-%d-*-*-p-*-iso8859-7serif.5a-monotype-times-regular-r-normal--*-%d-*-*-p-*-iso8859-9serif.6a-monotype-times-regular-r-normal--*-%d-*-*-p-*-iso8859-15serif.7a-hanyi-ming-medium-r-normal--*-%d-*-*-m-*-big5-1serif.8a-sun-song-medium-r-normal--*-%d-*-*-c-*-gb2312.1980-0serif.9a-ricoh-hgminchol-medium-r-normal--*-%d-*-*-m-*-jisx0201.1976-0serif.10a-ricoh-hgminchol-medium-r-normal--*-%d-*-*-m-*-jisx0208.1983-0serif.11a-ricoh-heiseimin-w3-r-normal--*-%d-*-*-m-*-jisx0212.1990-0serif.12a-hanyang-myeongjo-medium-r-normal--*-%d-*-*-m-*-ksc5601.1992-3serif.13a-urw-itczapfdingbats-medium-r-normal--*-%d-*-*-p-*-sun-fontspecificserif.14a-*-symbol-medium-r-normal--*-%d-*-*-p-*-sun-fontspecificbserif.italic.0=-monotype-timesbnewbroman-regular-i---*-%d-*-*-p-*-iso8859-1bserif.italic.1=-monotype-timesbnewbroman-regular-i-normal-italic-*-%d-*-*-p-*-iso8859-2",
        "-b&h-LucidaBrightLat4-normal-i-normal-Italic-*-%d-*-*-p-*-iso8859-4"
    };

    static String[] values3 = {
        "-monotype-times new roman-regular-r---*-%d-*-*-p-*-iso8859-1, -monotype-times new roman-regular-r-normal--*-%d-*-*-p-*-iso8859-2, -b&h-LucidaBrightLat4-Normal-r-normal--*-%d-*-*-p-*-iso8859-4, -monotype-times-regular-r-normal--*-%d-*-*-p-*-iso8859-5, -monotype-times new roman greek-regular-r-normal--*-%d-*-*-p-*-iso8859-7, -monotype-times-regular-r-normal--*-%d-*-*-p-*-iso8859-9, -monotype-times-regular-r-normal--*-%d-*-*-p-*-iso8859-15, -hanyi-ming-medium-r-normal--*-%d-*-*-m-*-big5-1, -sun-song-medium-r-normal--*-%d-*-*-c-*-gb2312.1980-0, -ricoh-hg gothic b-medium-r-normal--*-%d-*-*-m-*-jisx0201.1976-0, -ricoh-hg gothic b-medium-r-normal-*-*-%d-*-*-m-*-jisx0208.1983-0, -ricoh-heiseimin-w3-r-normal--*-%d-*-*-m-*-jisx0212.1990-0, -hanyang-myeongjo-medium-r-normal--*-%d-*-*-m-*-ksc5601.1992-3",
        "-monotype-times new roman-regular-i---*-%d-*-*-p-*-iso8859-1, -monotype-times new roman-regular-i-normal-italic-*-%d-*-*-p-*-iso8859-2, -b&h-LucidaBrightLat4-normal-i-normal-Italic-*-%d-*-*-p-*-iso8859-4, -monotype-times-regular-i-normal-italic-*-%d-*-*-p-*-iso8859-5, -monotype-times new roman greek-regular-i-normal--*-%d-*-*-p-*-iso8859-7, -monotype-times-regular-i-normal-italic-*-%d-*-*-p-*-iso8859-9, -monotype-times-regular-i-normal--*-%d-*-*-p-*-iso8859-15, -hanyi-ming-medium-r-normal--*-%d-*-*-m-*-big5-1, -sun-song-medium-r-normal--*-%d-*-*-c-*-gb2312.1980-0, -ricoh-hg gothic b-medium-r-normal--*-%d-*-*-m-*-jisx0201.1976-0, -ricoh-hg gothic b-medium-r-normal-*-*-%d-*-*-m-*-jisx0208.1983-0, -ricoh-heiseimin-w3-r-normal--*-%d-*-*-m-*-jisx0212.1990-0, -hanyang-myeongjo-medium-r-normal--*-%d-*-*-m-*-ksc5601.1992-3"
    };

    static String[][] keys = {keys1, keys1, keys2, keys2, keys3};
    static String[][] values = {values1, values1, values2, values2, values3};
    static String[] dfiles = {
        "testData1",
        "testData1.dos",
        "testData2",
        "testData2.dos",
        "testData3.dos"
    };
}
