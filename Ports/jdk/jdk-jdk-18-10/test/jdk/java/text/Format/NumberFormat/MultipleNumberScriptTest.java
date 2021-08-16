/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7073852 8008577
 * @summary Support multiple scripts for digits and decimal symbols per locale
 * @run main/othervm -Djava.locale.providers=JRE,SPI MultipleNumberScriptTest
 */

import java.text.*;
import java.util.*;

public class MultipleNumberScriptTest {

    static Locale[] locales = {
        new Locale("ar"),
        new Locale("ar", "EG"),
        new Locale("ar", "DZ"),
        Locale.forLanguageTag("ar-EG-u-nu-arab"),
        Locale.forLanguageTag("ar-EG-u-nu-latn"),
        Locale.forLanguageTag("ar-DZ-u-nu-arab"),
        Locale.forLanguageTag("ar-DZ-u-nu-latn"),
        Locale.forLanguageTag("ee"),
        Locale.forLanguageTag("ee-GH"),
        Locale.forLanguageTag("ee-GH-u-nu-latn"),
        new Locale("th", "TH", "TH"),
        Locale.forLanguageTag("th-TH"),
        Locale.forLanguageTag("th-TH-u-nu-thai"),
        Locale.forLanguageTag("th-TH-u-nu-hoge"),
    };

    // expected numbering system for each locale
    static String[] expectedNumSystem = {
        "latn", // ar
        "latn", // ar-EG
        "latn", // ar-DZ
        "arab", // ar-EG-u-nu-arab
        "latn", // ar-EG-u-nu-latn
        "arab", // ar-DZ-u-nu-arab
        "latn", // ar-DZ-u-nu-latn
        "latn", // ee
        "latn", // ee-GH
        "latn", // ee-GH-u-nu-latn
        "thai", // th_TH_TH
        "latn", // th-TH
        "thai", // th-TH-u-nu-thai
        "latn", // th-TH-u-nu-hoge (invalid)
    };

    public static void main(String[] args) {
        int num = 123456;

        for (int i = 0; i < locales.length; i++) {
            NumberFormat nf = NumberFormat.getIntegerInstance(locales[i]);
            String formatted = nf.format(num);
            System.out.printf("%s is %s in %s locale (expected in %s script).\n",
                num, formatted, locales[i], expectedNumSystem[i]);
            if (!checkResult(formatted, expectedNumSystem[i])) {
                throw new RuntimeException("test failed. expected number system was not returned.");
            }
        }
    }

    static boolean checkResult(String formatted, String numSystem) {
        switch (numSystem) {
            case "arab":
                return formatted.charAt(0) == '\u0661';
            case "latn":
                return formatted.charAt(0) == '1';
            case "thai":
                return formatted.charAt(0) == '\u0e51';
            default:
                return false;
        }
    }
}
