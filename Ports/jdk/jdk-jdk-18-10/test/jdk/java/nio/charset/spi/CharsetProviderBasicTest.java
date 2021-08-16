/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4429040 4591027 4814743
 * @summary Unit test for charset providers
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        jdk.test.lib.util.JarUtils
 *        FooCharset FooProvider CharsetTest
 * @run driver SetupJar
 * @run testng CharsetProviderBasicTest
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;

public class CharsetProviderBasicTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final List DEFAULT_CSS = List.of(
        "US-ASCII", "8859_1", "iso-ir-6", "UTF-16", "windows-1252", "!BAR", "cp1252"
    );

    private static final List MINIMAL_POLICY = List.of(
        "-Djava.security.manager",
        "-Djava.security.policy=" + TEST_SRC + File.separator + "default-pol"
    );

    private static final List CP_POLICY = List.of(
        "-Djava.security.manager",
        "-Djava.security.policy=" + TEST_SRC + File.separator + "charsetProvider.sp"
    );

    private static boolean checkSupports(String locale) throws Throwable {
        return ProcessTools.executeProcess("sh", "-c", "LC_ALL=" + locale + " && "
                                           + "locale -a | grep " + locale)
                           .getStdout()
                           .replace(System.lineSeparator(), "")
                           .equals(locale);
    }

    @DataProvider
    public static Iterator<Object[]> testCases() {
        return Stream.of("", "ja_JP.eucJP", "tr_TR")
                     .flatMap(locale -> Stream.of(
                             new Object[]{locale, List.of(""), "FOO"},
                             new Object[]{locale, MINIMAL_POLICY, "!FOO"},
                             new Object[]{locale, CP_POLICY, "FOO"}
                     ))
                     .iterator();
    }

    @Test(dataProvider = "testCases")
    public void testDefaultCharset(String locale, List opts, String css) throws Throwable {
        if ((System.getProperty("os.name").startsWith("Windows") || !checkSupports(locale))
                && (!locale.isEmpty())) {
            System.out.println(locale + ": Locale not supported, skipping...");
            return;
        }

        List<String> args = new ArrayList<>();
        args.add(JDKToolFinder.getJDKTool("java"));
        args.addAll(asList(Utils.getTestJavaOpts()));
        args.add("-cp");
        args.add(System.getProperty("test.class.path") + File.pathSeparator + "test.jar");
        args.addAll(opts);
        args.add(CharsetTest.class.getName());
        args.addAll(DEFAULT_CSS);
        args.add(css);
        args.removeIf(t -> t.isEmpty());

        ProcessBuilder pb = new ProcessBuilder(args);

        if (!locale.isEmpty()) {
            pb.environment().put("LC_ALL", locale);
        }

        ProcessTools.executeCommand(pb)
                    .shouldHaveExitValue(0);
    }
}
