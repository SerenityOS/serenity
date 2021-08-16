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
/*
 * @test
 * @bug 4303146 5102289 6272060
 * @summary Test non-standard loading strategies with ResourceBundle.Control subclasses
 */

import java.io.*;
import java.util.*;

public class LoadingStrategiesTest {

    static int errors;

    public static void main(String[] args) {
        ResourceBundle rb;
        String s;

        // Test zh_TW -> root, zh_CN -> zh -> root
        rb = ResourceBundle.getBundle("Chinese", Locale.TAIWAN, new ChineseControl());
        s = rb.getString("data");
        check("chinese with Locale.TAIWAN", s, "root");

        rb = ResourceBundle.getBundle("Chinese", Locale.CHINA, new ChineseControl());
        s = rb.getString("data");
        check("chinese with Locale.CHINA", s, "zh");


        // Test use of per-locale packaging
        preparePerLocalePackageProperties();

        rb = ResourceBundle.getBundle("test.package.Messages", Locale.US,
                                      new PerLocalePackageControl());
        s = rb.getString("data");
        check("Per-locale package with Locale.US", s, "");

        rb = ResourceBundle.getBundle("test.package.Messages", Locale.GERMAN,
                                      new PerLocalePackageControl());
        s = rb.getString("data");
        check("Per-locale package with Locale.GERMAN", s, "de");

        rb = ResourceBundle.getBundle("test.package.Messages", Locale.JAPAN,
                                      new PerLocalePackageControl());
        s = rb.getString("data");
        check("Per-locale package with Locale.JAPAN", s, "ja_JP");


        // Check any errors
        if (errors > 0) {
            throw new RuntimeException("FAILED: " + errors + " error(s)");
        }
    }

    private static void check(String msg, String got, String expected) {
        if (!got.equals(expected)) {
            error("%s: got \"%s\", expected \"%s\"%n", msg, got, expected);
        }
    }

    private static class ChineseControl extends ResourceBundle.Control {
        @Override
        public List<Locale> getCandidateLocales(String baseName,
                                                Locale locale) {
            if (locale.equals(Locale.TAIWAN)) {
                return Arrays.asList(locale,
                                     // no Locale("zh")
                                     new Locale(""));
            }
            return super.getCandidateLocales(baseName, locale);
        }
    }

    private static class PerLocalePackageControl extends ResourceBundle.Control {
        @Override
        public String toBundleName(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            String loc = super.toBundleName("", locale);
            if (loc.length() > 0) {
                return baseName.replaceFirst("^([\\w\\.]+)\\.(\\w+)$",
                                             "$1." + loc.substring(1) + ".$2");
            }
            return baseName;
        }

        // Avoid fallback to the default locale (6272060)
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            return null;
        }
    }

    // Creates:
    //     test/package/Messages.properties
    //     test/package/de/Messages.properties
    //     test/package/ja_JP/Messages.properties
    private static void preparePerLocalePackageProperties() {
        final String DEL = File.separator;
        try {
            String dir = System.getProperty("test.classes", ".");
            String[] subdirs = { "", "de", "ja_JP" };
            for (String subdir : subdirs) {
                StringBuilder sb = new StringBuilder();
                sb.append(dir).append(DEL).append("test").append(DEL).append("package");
                if (subdir.length() > 0) {
                    sb.append(DEL).append(subdir);
                }
                File path = new File(sb.toString());
                path.mkdirs();
                File propsfile = new File(path, "Messages.properties");
                OutputStream os = new FileOutputStream(propsfile);
                Properties props = new Properties();
                props.setProperty("data", subdir);
                props.store(os, null);
                System.out.println("Created: " + propsfile);
                os.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Can't set up per-locale properties", e);
        }
    }

    private static void error(String msg) {
        System.out.println(msg);
        errors++;
    }

    private static void error(String fmt, Object... args) {
        System.out.printf(fmt, args);
        errors++;
    }
}
