/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
/**
 * This is a simple program that will generate a key list that can be used as the basis
 * for a LocaleData file suitable for use with LocaleDataTest.  It always sends its
 * output to standard out.
 */
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Enumeration;
import java.io.PrintStream;

public class GenerateKeyList {
    public static void main(String[] args) throws Exception {
        doOutputFor("sun.util.resources", "CalendarData", System.out);
        doOutputFor("sun.util.resources", "CurrencyNames", System.out);
        doOutputFor("sun.util.resources", "LocaleNames", System.out);
        doOutputFor("sun.util.resources", "TimeZoneNames", System.out);
        doOutputFor("sun.text.resources", "CollationData", System.out);
        doOutputFor("sun.text.resources", "FormatData", System.out);
    };

    public static void doOutputFor(String packageName,
            String resourceBundleName, PrintStream out)
                    throws Exception {
        Locale[] availableLocales = Locale.getAvailableLocales();

        ResourceBundle bundle = ResourceBundle.getBundle(packageName +
                        resourceBundleName, new Locale("", "", ""));
        dumpResourceBundle(resourceBundleName + "/", bundle, out);
        for (int i = 0; i < availableLocales.length; i++) {
            bundle = ResourceBundle.getBundle(packageName + resourceBundleName,
                            availableLocales[i]);
            dumpResourceBundle(resourceBundleName + "/" + availableLocales[i].toString(),
                            bundle, out);
        }
    }

    public static void dumpResourceBundle(String pathName, ResourceBundle bundle,
                    PrintStream out) throws Exception {
        Enumeration keys = bundle.getKeys();
        while(keys.hasMoreElements()) {
            String key = (String)(keys.nextElement());
            dumpResource(pathName + "/" + key, bundle.getObject(key), out);
        }
    }

    public static void dumpResource(String pathName, Object resource, PrintStream out)
                    throws Exception {
        if (resource instanceof String[]) {
            String[] stringList = (String[])resource;
            for (int i = 0; i < stringList.length; i++)
                out.println(pathName + "/" + i);
        }
        else if (resource instanceof String[][]) {
            String[][] stringArray = (String[][])resource;
            if (pathName.startsWith("TimeZoneNames")) {
                for (int i = 0; i < stringArray.length; i++)
                    for (int j = 0; j < stringArray[i].length; j++)
                        out.println(pathName + "/" + i + "/" + j);
            }
            else {
                for (int i = 0; i < stringArray.length; i++)
                    out.println(pathName + "/" + stringArray[i][0]);
            }
        }
        else
            out.println(pathName);
    }
}
