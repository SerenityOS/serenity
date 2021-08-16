/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7125442
 * @summary ensures a jar path as well as a class located in a path containing
 *          unicode characters are launched.
 * @compile -XDignore.symbol.file I18NJarTest.java
 * @run main/othervm I18NJarTest
 */
import java.io.File;
import java.util.Locale;

/*
 * Note 1: the system must have the correct Locale, in order for the test to
 * work correctly, on those systems that do not comply, the test will succeed.
 * Here are some guidelines to set the locale correctly.
 * On Windows: ControlPanel->Regional Settings,
 *             ensure that "Japanese" is selected and checked, and in
 *             "Code page conversion tables", the following must be checked,
 *             932 (ANSI/OEM - Japanese Shift-JIS).
 * On Unix: use "locale -a" verify one of these exist ja_JP.UTF-8 or
 *          ja_JP.utf8 or ja_JP.ujis, and export one of them with LC_ALL.
 *
 *
 * Note 2: since we need to set the locale, it is safest to execute this test
 * in its own VM (othervm mode), such that the ensuing tests can run unperturbed,
 * regardless of the outcome.
 */
public class I18NJarTest extends TestHelper {
    private static final File cwd = new File(".");
    private static final File dir = new File("\uFF66\uFF67\uFF68\uFF69");
    private static final String encoding = System.getProperty("sun.jnu.encoding", "");
    private static final String LANG = System.getenv("LANG");
    private static final String LC_ALL = System.getenv("LC_ALL");

    public static void main(String... args) throws Exception {
        boolean localeAvailable = false;
        for (Locale l : Locale.getAvailableLocales()) {
            if (l.toLanguageTag().equals(Locale.JAPAN.toLanguageTag())) {
                localeAvailable = true;
                break;
            }
        }
        if (!localeAvailable) {
            System.out.println("Warning: locale: " + Locale.JAPAN
                    + " not found, test passes vacuously");
            return;
        }
        if ("C".equals(LC_ALL) || "C".equals(LANG)) {
            System.out.println("Warning: The LANG and/or LC_ALL env vars are " +
              "set to \"C\":\n" +
              "  LANG=" + LANG + "\n" +
              "  LC_ALL=" + LC_ALL + "\n" +
              "This test requires support for multi-byte filenames.\n" +
              "Test passes vacuously.");
            return;
        }
        if (encoding.equals("MS932") || encoding.equals("UTF-8")) {
            Locale.setDefault(Locale.JAPAN);
            System.out.println("using locale " + Locale.JAPAN +
                    ", encoding " + encoding);
        } else {
            System.out.println("Warning: current encoding is " + encoding +
                    "this test requires MS932 <Ja> or UTF-8," +
                    " test passes vacuously");
            return;
        }
        dir.mkdir();
        File dirfile = new File(dir, "foo.jar");
        createJar(dirfile,
                "public static void main(String... args) {",
                "System.out.println(\"Hello World\");",
                "System.exit(0);",
                "}");

        // remove the class files, to ensure that the class is indeed picked up
        // from the jar file and not from ambient classpath.
        File[] classFiles = cwd.listFiles(createFilter(CLASS_FILE_EXT));
        for (File f : classFiles) {
            f.delete();
        }

        // test with a jar file
        TestResult tr = doExec(javaCmd, "-jar", dirfile.getAbsolutePath());
        System.out.println(tr);
        if (!tr.isOK()) {
            throw new RuntimeException("TEST FAILED");
        }

        // test the same class but by specifying it as a classpath
        tr = doExec(javaCmd, "-cp", dirfile.getAbsolutePath(), "Foo");
        System.out.println(tr);
        if (!tr.isOK()) {
            throw new RuntimeException("TEST FAILED");
        }
    }
}
